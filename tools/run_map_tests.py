"""Run MapCheck, bring the app back from HOME and capture actual Android screens. Requires adb."""
import argparse, os, subprocess, sys, time, socket, struct, binascii, zlib
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('serial');parser.add_argument('--test-class',default='MapCheck',choices=['MapCheck','EgoLineCheck','IettCheck','RoutePinCheck','WidgetCheck','StorageCheck','ThemeCheck','MapSeamCheck','RailCheck','AntalyaCheck']);parser.add_argument('--output',type=Path,default=Path('test-results'));args=parser.parse_args()
adb=os.environ.get('ANDROID_ADB','adb');args.output.mkdir(parents=True,exist_ok=True)
def capture(name):
    sdk=int(subprocess.check_output([adb,'-s',args.serial,'shell','getprop','ro.build.version.sdk']).strip())
    if sdk>=14:
        png=subprocess.check_output([adb,'-s',args.serial,'exec-out','screencap','-p'])
    else:
        with socket.create_connection(('127.0.0.1',int(os.environ.get('ANDROID_ADB_SERVER_PORT','5037'))),15) as sock:
            def read(n):
                data=b''
                while len(data)<n:
                    piece=sock.recv(n-len(data))
                    if not piece:raise EOFError('ADB framebuffer')
                    data+=piece
                return data
            def command(text):
                data=text.encode();sock.sendall(('%04x'%len(data)).encode()+data)
                if read(4)!=b'OKAY':raise RuntimeError('ADB request failed')
            command('host:transport:'+args.serial);command('framebuffer:')
            if struct.unpack('<I',read(4))[0]!=1:raise RuntimeError('Unsupported framebuffer')
            bpp,size,w,h,ro,rl,bo,bl,go,gl,ao,al=struct.unpack('<12I',read(48))
            if size>16777216 or bpp not in (16,24,32):raise RuntimeError('Unsupported framebuffer size')
            raw=read(size);step=bpp//8;pixels=bytearray(w*h*3)
            for i in range(w*h):
                n=int.from_bytes(raw[i*step:(i+1)*step],'little')
                for j,(offset,length) in enumerate(((ro,rl),(go,gl),(bo,bl))):pixels[i*3+j]=((n>>offset)&((1<<length)-1))*255//((1<<length)-1)
            def chunk(tag,data):return struct.pack('>I',len(data))+tag+data+struct.pack('>I',binascii.crc32(tag+data)&0xffffffff)
            scan=b''.join(b'\x00'+pixels[y*w*3:(y+1)*w*3] for y in range(h))
            png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(scan))+chunk(b'IEND',b'')
    if not png.startswith(b'\x89PNG'):raise RuntimeError('Invalid screenshot')
    (args.output/(args.serial+'-'+name+'.png')).write_bytes(png)
p=subprocess.Popen([adb,'-s',args.serial,'shell','am','instrument','-w','org.transittr.tests/.'+args.test_class],stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
passed=False;log=[]
for raw in iter(p.stdout.readline,b''):
    line=raw.decode('utf-8','replace');log.append(line);print(line,end='',flush=True)
    if 'RESUME_FROM_HOME' in line:subprocess.run([adb,'-s',args.serial,'shell','am','start','-f','0x10200000','-n','org.transittr/.TransitActivity'],capture_output=True,check=True)
    if 'CAPTURE=' in line:time.sleep(3);capture(line.split('CAPTURE=',1)[1].strip())
    if 'PASS Android' in line:passed=True
code=p.wait();(args.output/(args.serial+'-test.txt')).write_text(''.join(log),encoding='utf-8')
sys.exit(0 if passed and code==0 else 1)
