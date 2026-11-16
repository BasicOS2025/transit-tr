"""Exercise TV 1.7 D-pad focus on an already booted test emulator."""
import os, subprocess, sys, time, xml.etree.ElementTree as ET
serial=sys.argv[1] if len(sys.argv)>1 else "emulator-5562"
adb=os.environ.get("ANDROID_ADB","adb")
def shell(*args):
 return subprocess.check_output([adb,"-s",serial,"shell",*map(str,args)],stderr=subprocess.STDOUT)
def focus():
 shell("uiautomator","dump","/sdcard/transit-focus.xml")
 root=ET.fromstring(shell("cat","/sdcard/transit-focus.xml"))
 active=[x for x in root.iter("node") if x.get("focused")=="true"]
 if len(active)!=1: raise AssertionError("Expected one focused element: "+str(len(active)))
 return active[0]
def press(code):
 shell("input","keyevent",code)
 return focus()
def expect(node,text=None,kind=None,description=None):
 if text is not None and node.get("text")!=text: raise AssertionError(("text",text,node.attrib))
 if kind is not None and node.get("class")!=kind: raise AssertionError(("class",kind,node.attrib))
 if description is not None and description not in node.get("content-desc",""): raise AssertionError(("description",description,node.attrib))
shell("input","keyevent",20)  # Leave touch mode as a TV remote does.
shell("am","force-stop","org.transittr.tv")
shell("am","start","-n","org.transittr.tv/org.transittr.TvActivity")
node=None
for attempt in range(12):
 try:node=focus()
 except (AssertionError,ET.ParseError,subprocess.CalledProcessError):
  time.sleep(.5);continue
 if node.get("package")=="org.transittr.tv" and node.get("text")=="Başlat":break
 time.sleep(.5)
else:raise AssertionError("TV app did not reach its initial focus: "+str(node.attrib if node is not None else None))
expect(press(21),kind="android.widget.EditText")
expect(press(21),kind="android.widget.EditText")
expect(press(22),kind="android.widget.EditText")
expect(press(22),text="Başlat")
press(20);expect(press(20),text="Ayarlar")
for label in ("Katalog","Metro","Otobüs"): expect(press(21),text=label)
expect(press(19),kind="android.view.View")
expect(press(23),description="Harita hareket modu")
expect(press(22),description="Harita hareket modu")
expect(press(4),description="Harita. Taşımak")
expect(press(20),kind="android.widget.TextView")
print("PASS TV remote: fields, navigation, map pan and exit")
services=shell("pm","query-services","-a","android.service.dreams.DreamService").decode("utf-8","replace")
if "org.transittr.TvDream" not in services:raise AssertionError("TV dream missing from package manager")
print("PASS TV screensaver service discovery")
