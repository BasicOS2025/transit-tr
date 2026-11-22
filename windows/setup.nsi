Unicode true
!include "LogicLib.nsh"
!include "WinVer.nsh"

!ifndef PRODUCT_DIR
  !error "Define PRODUCT_DIR as the absolute path to the built application."
!endif
!ifndef OUTPUT_FILE
  !define OUTPUT_FILE "setup.exe"
!endif

Name "Transit TR 1.0"
OutFile "${OUTPUT_FILE}"
InstallDir "$LOCALAPPDATA\Programs\Transit TR"
InstallDirRegKey HKCU "Software\TransitTR" "InstallDir"
RequestExecutionLevel user
SetCompressor /SOLID lzma
VIProductVersion "1.0.0.0"
VIAddVersionKey "ProductName" "Transit TR"
VIAddVersionKey "FileDescription" "Transit TR Setup"
VIAddVersionKey "FileVersion" "1.0.0"
VIAddVersionKey "ProductVersion" "1.0.0"
Page directory
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles

Function .onInit
  ${IfNot} ${AtLeastWin7}
    MessageBox MB_OK|MB_ICONSTOP "Windows 7 SP1 or later is required."
    Abort
  ${EndIf}
  ${If} ${IsWin7}
  ${AndIfNot} ${AtLeastServicePack} 1
    MessageBox MB_OK|MB_ICONSTOP "Windows 7 Service Pack 1 is required."
    Abort
  ${EndIf}
FunctionEnd

Section "Transit TR"
  ReadRegDWORD $0 HKLM "SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full" "Release"
  ${If} $0 < 528040
    SetRegView 64
    ReadRegDWORD $0 HKLM "SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full" "Release"
    SetRegView 32
  ${EndIf}
  ${If} $0 < 528040
    MessageBox MB_OK|MB_ICONSTOP ".NET Framework 4.8 is required. Install it from Microsoft, then open setup again."
    SetErrorLevel 2
    Abort
  ${EndIf}
  SetShellVarContext current
  SetOutPath "$INSTDIR"
  File "${PRODUCT_DIR}\TransitTR.exe"
  File "${PRODUCT_DIR}\TransitTR.exe.config"
  File "${PRODUCT_DIR}\LICENSE"
  File "${PRODUCT_DIR}\THIRD-PARTY-NOTICES.md"
  File "${PRODUCT_DIR}\README.md"
  SetOutPath "$INSTDIR\data"
  File "${PRODUCT_DIR}\data\*.json.gz"
  SetOutPath "$INSTDIR"
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  CreateDirectory "$SMPROGRAMS\Transit TR"
  CreateShortcut "$SMPROGRAMS\Transit TR\Transit TR.lnk" "$INSTDIR\TransitTR.exe"
  CreateShortcut "$SMPROGRAMS\Transit TR\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKCU "Software\TransitTR" "InstallDir" "$INSTDIR"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\TransitTR" "DisplayName" "Transit TR"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\TransitTR" "DisplayVersion" "1.0"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\TransitTR" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\TransitTR" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\TransitTR" "NoRepair" 1
SectionEnd

Section "Uninstall"
  SetShellVarContext current
  Delete "$SMPROGRAMS\Transit TR\Transit TR.lnk"
  Delete "$SMPROGRAMS\Transit TR\Uninstall.lnk"
  RMDir "$SMPROGRAMS\Transit TR"
  Delete "$INSTDIR\TransitTR.exe"
  Delete "$INSTDIR\TransitTR.exe.config"
  Delete "$INSTDIR\LICENSE"
  Delete "$INSTDIR\THIRD-PARTY-NOTICES.md"
  Delete "$INSTDIR\README.md"
  Delete "$INSTDIR\data\*.json.gz"
  RMDir "$INSTDIR\data"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir "$INSTDIR"
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\TransitTR"
  DeleteRegValue HKCU "Software\TransitTR" "InstallDir"
  DeleteRegKey /ifempty HKCU "Software\TransitTR"
SectionEnd
