"""Build API 8 APK with ECJ, R8, aapt, zipalign and apksigner; no network access."""
from pathlib import Path
import os,subprocess,tempfile,zipfile,ctypes,shutil
ROOT=Path(__file__).resolve().parents[1]
def short(p):
 p=str(Path(p).resolve())
 if os.name=='nt':
  buf=ctypes.create_unicode_buffer(32768)
  if ctypes.windll.kernel32.GetShortPathNameW(p,buf,len(buf)):return buf.value
 return p
def need(name):
 if not os.environ.get(name):raise SystemExit('Set '+name)
 return short(os.environ[name])
def run(*args):subprocess.run([str(x) for x in args],check=True)
jar=need('ANDROID_JAR');ecj=need('ECJ_JAR');bt=Path(need('BUILD_TOOLS'));key=need('KEYSTORE')
if not os.environ.get('KS_PASS'):raise SystemExit('Set KS_PASS')
java=os.environ.get('JAVA_EXE','java');root=Path(short(ROOT));(root/'build').mkdir(exist_ok=True)
work=Path(tempfile.mkdtemp(prefix='android-',dir=str(root/'build')));classes=work/'classes';classes.mkdir();dex=work/'dex';dex.mkdir();dist=root/'dist';dist.mkdir(exist_ok=True)
tests_mode=os.environ.get('ANDROID_TESTS')=='1'
source=root/('android-tests' if tests_mode else 'android-legacy');library=root/'android-legacy/libs/transit-tls.jar'
run(java,'-jar',ecj,'-1.6','-encoding','UTF-8','-bootclasspath',jar,'-classpath',library,'-d',classes,*sorted((source/'src').rglob('*.java')))
program=work/'program.jar'
with zipfile.ZipFile(program,'w',zipfile.ZIP_DEFLATED) as z:
 for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes))
rules=root/'tools/android-r8.pro'
if tests_mode:
 rules=work/'test-rules.pro';rules.write_text('-keep class org.transittr.tests.** { *; }\n-dontobfuscate\n',encoding='utf-8')
run(java,'-cp',bt/'lib/d8.jar','com.android.tools.r8.R8','--release','--min-api','8','--lib',jar,'--pg-conf',rules,'--output',dex,program,*([] if tests_mode else [library]))
modern=not tests_mode and os.environ.get('ANDROID_VARIANT','legacy')=='modern'
manifest=work/'AndroidManifest.xml';text=(source/'AndroidManifest.xml').read_text(encoding='utf-8')
if modern:text=text.replace('android:minSdkVersion="8"','android:minSdkVersion="23"').replace('android:targetSdkVersion="8"','android:targetSdkVersion="28"')
manifest.write_text(text,encoding='utf-8')
exe='.exe' if os.name=='nt' else '';unsigned=work/'unsigned.apk'
resources=[]
if (source/'assets').exists():resources+=['-A',source/'assets']
if (source/'res').exists():resources+=['-S',source/'res']
run(bt/('aapt'+exe),'package','-f','-0','sqlite','-M',manifest,'-I',jar,*resources,'-F',unsigned)
with zipfile.ZipFile(unsigned,'a',zipfile.ZIP_DEFLATED) as z:z.write(dex/'classes.dex','classes.dex')
aligned=work/'aligned.apk';run(bt/('zipalign'+exe),'-f','4',unsigned,aligned)
apk=dist/('transit-tr-v0.9-'+('tests' if tests_mode else 'modern' if modern else 'legacy')+'.apk');minimum='23' if modern else '8'
run(java,'-jar',bt/'lib/apksigner.jar','sign','--min-sdk-version',minimum,'--ks',key,'--ks-key-alias',os.environ.get('KEY_ALIAS','transittr'),'--ks-pass','env:KS_PASS','--key-pass','env:KS_PASS','--out',apk,aligned)
run(java,'-jar',bt/'lib/apksigner.jar','verify','--verbose','--min-sdk-version',minimum,apk)
print('APK:',apk)
