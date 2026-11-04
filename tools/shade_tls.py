from pathlib import Path
import struct,zipfile,hashlib
src=Path(__file__).resolve().parents[1];out=src/'android-legacy/libs';out.mkdir(exist_ok=True)
def shade(data):
 cp=struct.unpack('>H',data[8:10])[0];pos=10;result=bytearray(data[:10]);i=1
 while i<cp:
  tag=data[pos];pos+=1;result.append(tag)
  if tag==1:
   n=struct.unpack('>H',data[pos:pos+2])[0];pos+=2;x=data[pos:pos+n];pos+=n;x=x.replace(b'org/bouncycastle',b'org/transittr/bc').replace(b'org.bouncycastle',b'org.transittr.bc');result+=struct.pack('>H',len(x))+x
  else:
   n={3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}[tag];result+=data[pos:pos+n];pos+=n
   if tag in (5,6):i+=1
  i+=1
 return bytes(result)+data[pos:]
with zipfile.ZipFile(out/'transit-tls.jar','w',zipfile.ZIP_DEFLATED) as target:
 seen=set()
 for name in ['bcprov','bctls','bcutil']:
  p=src/'vendor-inputs'/(name+'.jar');print(name,hashlib.sha256(p.read_bytes()).hexdigest())
  with zipfile.ZipFile(p) as z:
   for f in z.namelist():
    if f.startswith('org/bouncycastle/') and f.endswith('.class'):
     new=f.replace('org/bouncycastle/','org/transittr/bc/')
     if new not in seen:target.writestr(new,shade(z.read(f)));seen.add(new)
print('Relocated',len(seen),'classes')
