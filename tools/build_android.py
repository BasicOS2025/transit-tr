"""Build API 8 APK with ECJ, R8, aapt, zipalign and apksigner; no network access."""
from pathlib import Path
import os,subprocess,tempfile,zipfile,ctypes,shutil,re,gzip
ROOT=Path(__file__).resolve().parents[1]
if os.environ.get('TRANSIT_DISTRIBUTION')=='public':
 from legal_release_check import enforce
 enforce(ROOT)
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
modern=not tests_mode and os.environ.get('ANDROID_VARIANT','legacy')=='modern'
modern_jar=None
if modern:
 modern_jar=short(os.environ.get('ANDROID_MODERN_JAR',str(bt.parent.parent/'platforms/android-34/android.jar')))
 if not Path(modern_jar).exists():raise SystemExit('Set ANDROID_MODERN_JAR to Android API 26+ android.jar')
source=root/('android-tests' if tests_mode else 'android-legacy');library=root/'android-legacy/libs/transit-tls.jar'
run(java,'-jar',ecj,'-1.6','-encoding','UTF-8','-bootclasspath',jar,'-classpath',library,'-d',classes,*sorted((source/'src').rglob('*.java')))
if modern:run(java,'-jar',ecj,'-1.6','-encoding','UTF-8','-bootclasspath',modern_jar,'-classpath',classes,'-d',classes,*sorted((root/'android-modern/src').rglob('*.java')))
program=work/'program.jar'
with zipfile.ZipFile(program,'w',zipfile.ZIP_DEFLATED) as z:
 for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes))
rules=root/'tools/android-r8.pro'
if tests_mode:
 rules=work/'test-rules.pro';rules.write_text('-keep class org.transittr.tests.** { *; }\n-dontobfuscate\n',encoding='utf-8')
run(java,'-cp',bt/'lib/d8.jar','com.android.tools.r8.R8','--release','--min-api','8','--lib',modern_jar if modern else jar,'--pg-conf',rules,'--output',dex,program,*([] if tests_mode else [library]))
manifest=work/'AndroidManifest.xml';text=(source/'AndroidManifest.xml').read_text(encoding='utf-8')
if modern:text=text.replace('android:icon="@drawable/icon"','android:icon="@drawable/icon" android:roundIcon="@drawable/icon"').replace('android:minSdkVersion="8"','android:minSdkVersion="23"').replace('android:targetSdkVersion="8"','android:targetSdkVersion="28"').replace('</application>','<service android:name=".WidgetJobService" android:permission="android.permission.BIND_JOB_SERVICE" android:exported="true"/><service android:name=".ApproachJobService" android:permission="android.permission.BIND_JOB_SERVICE" android:exported="true"/></application>')
manifest.write_text(text,encoding='utf-8')
exe='.exe' if os.name=='nt' else '';unsigned=work/'unsigned.apk'
resources=[]
if (source/'assets').exists():
 assets=work/'assets';shutil.copytree(source/'assets',assets,ignore=shutil.ignore_patterns('*.sqlite','ego-times'))
 resources+=['-A',assets]
if (source/'res').exists():
 res=source/'res'
 if modern:
  res=work/'res';shutil.copytree(source/'res',res)
  shutil.copytree(root/'android-modern/res',res,dirs_exist_ok=True)
  widget=res/'xml/transit_widget.xml'
  widget.write_text(widget.read_text(encoding='utf-8').replace('/>','android:resizeMode="horizontal|vertical" android:minResizeWidth="180dp" android:minResizeHeight="50dp" />'),encoding='utf-8')
 resources+=['-S',res]
run(bt/('aapt'+exe),'package','-f','-0','dbz','-M',manifest,'-I',modern_jar if modern else jar,*resources,'-F',unsigned)
with zipfile.ZipFile(unsigned,'a',zipfile.ZIP_DEFLATED) as z:z.write(dex/'classes.dex','classes.dex')
aligned=work/'aligned.apk';run(bt/('zipalign'+exe),'-f','4',unsigned,aligned)
version=re.search(r'android:versionName="([^"]+)"',(root/'android-legacy/AndroidManifest.xml').read_text(encoding='utf-8')).group(1)
apk=dist/('transit-tr-v'+version+'-'+('tests' if tests_mode else 'modern' if modern else 'legacy')+'.apk');minimum='23' if modern else '8'
run(java,'-jar',bt/'lib/apksigner.jar','sign','--min-sdk-version',minimum,'--ks',key,'--ks-key-alias',os.environ.get('KEY_ALIAS','transittr'),'--ks-pass','env:KS_PASS','--key-pass','env:KS_PASS','--out',apk,aligned)
run(java,'-jar',bt/'lib/apksigner.jar','verify','--verbose','--min-sdk-version',minimum,apk)
print('APK:',apk)
