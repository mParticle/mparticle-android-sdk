package com.mparticle;

import java.io.IOException;
import java.io.InputStream;

public final class MPInputStream extends InputStream
{
  private ISocket a;
  private MeasuredRequest b;
  private InputStream c;


  public MPInputStream(ISocket paramae, InputStream paramInputStream)
  {
    if (paramae == null)
      throw new NullPointerException("socket was null");
    if (paramInputStream == null)
      throw new NullPointerException("delegate was null");

    this.a = paramae;
    this.c = paramInputStream;

  }

  public final int available() throws IOException
  {
    return this.c.available();
  }

  public final void close() throws IOException
  {
    try
    {
    //  this.e.f();
    }
    catch (ThreadDeath localThreadDeath)
    {
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {
    //  dm.a(localThrowable);
    }
    this.c.close();
  }

  public final void mark(int readlimit)
  {
    this.c.mark(readlimit);
  }

  public final boolean markSupported()
  {
    return this.c.markSupported();
  }

  private void a(Exception paramException)
  {
    try
    {
      Exception localException = paramException;
    //  paramException = this;
      MeasuredRequest localb;
      if ((localb = e()) != null)
      {
       // localb.g = cf.a(localException);
        //paramException.d.a(localb, MeasuredRequest.a.h);
      }
      return;
    }
    catch (ThreadDeath localThreadDeath)
    {
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {
     // dm.a(localThrowable);
    }
  }

  public final int read() throws IOException
  {
    int i;
    try
    {
      i = this.c.read();
    }
    catch (IOException localIOException)
    {
      a(localIOException);
      throw localIOException;
    }
    try
    {
  //    localObject = null;
    //  this.e.a(i);
    }
    catch (ThreadDeath localThreadDeath)
    {
      Object localObject = null;
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {
      //this.e = as.d;
      //dm.a(localThrowable);
    }
    return i;
  }

  public final int read(byte[] buffer) throws IOException
  {
    int i;
    try
    {
      i = this.c.read(buffer);
    }
    catch (IOException localIOException)
    {
      a(localIOException);
      throw localIOException;
    }
    //a(localIOException, 0, i);
    return i;
  }

  public final int read(byte[] buffer, int offset, int length) throws IOException
  {

    try
    {
      length = this.c.read(buffer, offset, length);
    }
    catch (IOException localIOException1)
    {
      a(localIOException1);
      throw localIOException1;
    }
 //   a(localIOException1, offset, length);
    return length;
  }

  private void a(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    try
    {
      int i = paramInt2;
      paramInt2 = paramInt1;
   //   paramInt1 = paramArrayOfByte;
     // paramArrayOfByte = null;
      //this.e.a(paramInt1, paramInt2, i);
      return;
    }
    catch (ThreadDeath localThreadDeath)
    {
      throw localThreadDeath;
    }
    catch (Throwable paramArrayOfBytes)
    {
    //  this.e = as.d;
     // dm.a(paramArrayOfByte);
    }
  }

  public final synchronized void reset() throws IOException
  {
    this.c.reset();
  }

  public final long skip(long byteCount) throws IOException
  {
    return this.c.skip(byteCount);
  }

  public final void a(String paramString1, String paramString2)
  {
  }

  public final void a(int paramInt)
  {
    MeasuredRequest localb;
    if ((localb = e()) != null)
    {
      localb.c();
      localb.e = paramInt;
    }
  }

  public final void a(Object paramaf)
  {
   // this.e = paramaf;
  }

  public final Object a()
  {
    return null;//this.e;
  }

  public final void b(int paramInt)
  {
    MeasuredRequest localb = null;
    if (this.b != null)
    {
      int i;
      if (((i = this.b.e) >= 100) && (i < 200))
      {
        (localb = new MeasuredRequest(this.b.a())).e(this.b.a);
        localb.d(this.b.d);
        localb.f = this.b.f;
      }
      this.b.b(paramInt);
    //  this.d.a(this.MeasuredRequest, MeasuredRequest.a.g);
    }
    this.b = localb;
  }

  private MeasuredRequest e()
  {
    if (this.b == null)
      this.b = this.a.b();
    return this.b;
  }

  public final Object b()
  {
    return null;
  }

  public final String c()
  {
    MeasuredRequest localb = e();
    String str = null;
    if (localb != null)
      str = localb.f;
    return str;
  }

  public final void a(String paramString)
  {
  }

  public final boolean a(InputStream paramInputStream)
  {
    return this.c == paramInputStream;
  }

  public final void d()
  {
   // if ((this.MeasuredRequest != null) && (this.MeasuredRequest.g == cf.a) && (this.e != null))
     // this.e.f();
  }
}