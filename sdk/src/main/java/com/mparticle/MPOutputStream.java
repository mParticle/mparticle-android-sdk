package com.mparticle;

import java.io.IOException;
import java.io.OutputStream;

public final class MPOutputStream extends OutputStream
{
  private ISocket a;
  private OutputStream b;
  private MeasuredRequest c;

  public MPOutputStream(ISocket paramae, OutputStream paramOutputStream)
  {
    if (paramae == null)
      throw new NullPointerException("socket was null");
    if (paramOutputStream == null)
      throw new NullPointerException("output stream was null");
    this.a = paramae;
    this.b = paramOutputStream;
    //this.d = MeasuredRequest();
    //if (this.d == null)
     // throw new NullPointerException("parser was null");
  }

  public final void flush() throws IOException
  {
    this.b.flush();
  }

  public final void close() throws IOException
  {
    this.b.close();
  }

  public final void write(int oneByte) throws IOException
  {
    this.b.write(oneByte);
    try
    {
     // this.d.a(oneByte);
      return;
    }
    catch (ThreadDeath localThreadDeath)
    {
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {
     // dm.a(localThrowable);
      //this.d = as.d;
    }
  }

  public final void write(byte[] buffer) throws IOException
  {
    this.b.write(buffer);
    if (buffer != null)
      a(buffer, 0, buffer.length);
  }

  public final void write(byte[] buffer, int offset, int byteCount) throws IOException
  {
    this.b.write(buffer, offset, byteCount);
    if (buffer != null)
      a(buffer, offset, byteCount);
  }

  private void a(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    try
    {
      //paramArrayOfByte = paramInt2;
      paramInt2 = paramInt1;
    //  paramInt1 = paramArrayOfByte;
      //this.d.a(paramInt1, paramInt2, paramArrayOfByte);
      return;
    }
    catch (ThreadDeath localThreadDeath)
    {
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {
    //  dm.a(localThrowable);
      //this.d = as.d;
    }
  }

  public final void a(String paramString1, String paramString2)
  {
    MeasuredRequest localb;
    (localb = d()).b();
    localb.f = paramString1;
    //(paramString1 = localb).i = null;
 //   paramString1 = paramString1.h;
    if (paramString2 != null)
    //  paramString1.c = paramString2;
    this.a.a(localb);
  }

  public final void a(int paramInt)
  {
  }

  public final void a(Object paramaf)
  {
 //   this.d = paramaf;
  }

  public final Object a()
  {
    return null;
  }

  public final void b(int paramInt)
  {
    MeasuredRequest localb = this.c;
    this.c = null;
    if (localb != null)
      localb.d(paramInt);
  }

  private MeasuredRequest d()
  {
    if (this.c == null)
      this.c = this.a.a();
    return this.c;
  }



  public final String c()
  {
    MeasuredRequest localb = d();
    String str = null;
    if (localb != null)
      str = localb.f;
    return str;
  }

  public final void a(String paramString)
  {
    MeasuredRequest localb;
    if ((localb = d()) != null)
      localb.a(paramString);
  }

  public final boolean a(OutputStream paramOutputStream)
  {
    return this.b == paramOutputStream;
  }
}