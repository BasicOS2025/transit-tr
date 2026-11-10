package org.transittr;
import android.content.Context;import java.io.*;
public final class LegalNotice {
 public static String read(Context context,String name){if(!name.equals("privacy.txt")&&!name.equals("terms.txt"))return "Bildirim bulunamadı.";try{InputStream in=context.getAssets().open(name);try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(out.size()+n>65536)throw new IOException("Bildirim sınırı");out.write(b,0,n);}return new String(out.toByteArray(),"UTF-8");}finally{in.close();}}catch(IOException e){return "Yayıncı: Ata Köse\nİletişim: atakose2012@gmail.com\nBildirim okunamadı. Kaynak paketindeki docs/PRIVACY-TR.md ve docs/TERMS-TR.md belgelerine bakın.";}}
}
