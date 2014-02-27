package com.mparticle.networking;

import android.location.Location;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import org.json.JSONArray;

public final class MeasuredRequest
{
    public long a = 9223372036854775807L;
    private long k = 9223372036854775807L;
    private boolean l = false;
    private boolean m = false;
    public boolean b = false;
 //   a c = a.a;
    private long n = 0L;
    public long d = 0L;
    private boolean o = false;
    private boolean p = false;
    public int e = 0;
    public String f = "";
   // public cf g = cf.a;
    private double[] q;
   // public k h = new k();
    public String i;
  //  public g j = g.a;

    public MeasuredRequest()
    {
    }

    public MeasuredRequest(String paramString)
    {
        if (paramString != null)
            this.i = paramString;
    }

    public MeasuredRequest(URL paramURL)
    {
        if (paramURL != null)
            this.i = paramURL.toExternalForm();
    }

    public final void a(long paramLong)
    {
        if (!this.o)
            this.n += paramLong;
    }

    public final void b(long paramLong)
    {
        this.o = true;
        this.n = paramLong;
    }

    public final void c(long paramLong)
    {
        if (!this.p)
            this.d += paramLong;
    }

    public final void d(long paramLong)
    {
        this.p = true;
        this.d = paramLong;
    }

    public final String a()
    {
      /*  Object localObject1;
        if ((localObject1 = this.i) == null)
        {
            Object localObject2 = localObject1 = this.h;
            String str1 = "unknown-host";
            if (((k)localObject2).MeasuredRequest != null)
                str1 = ((k)localObject2).MeasuredRequest;
            else if (((k)localObject2).a != null)
                str1 = ((k)localObject2).a.getHostName();
            int i1 = ((k)localObject1).e;
            localObject2 = str1;
            if (i1 > 0)
                localObject1 = ":" + i1;
            String str2 = ((k)localObject1).c;
            String str3 = "";
            if (((k)localObject1).d != null)
                str3 = str3 + k.a.a(((k)localObject1).d) + ":";
            str3 = str3 + "//";
            localObject2 = "";
            if ((((k)localObject1).e > 0) && ((((k)localObject1).d == null) || (k.a.MeasuredRequest(((k)localObject1).d) != ((k)localObject1).e)))
            {
                localObject1 = ":" + ((k)localObject1).e;
                if (!i1.endsWith((String)localObject1))
                    localObject2 = localObject1;
            }
            localObject1 = str3 + i1 + (String)localObject2 + str2;
            this.i = ((String)localObject1);
        }
        return localObject1;*/
        return "IMPLEMENT ME";
    }

    private long f()
    {
        long l1 = 9223372036854775807L;
        if ((this.a != 9223372036854775807L) && (this.k != 9223372036854775807L))
            l1 = this.k - this.a;
        return l1;
    }

    public final void e(long paramLong)
    {
        this.a = paramLong;
        this.l = true;
    }

    public final void b()
    {
        if ((!this.l) && (this.a == 9223372036854775807L))
            this.a = System.currentTimeMillis();
    }

    public final void f(long paramLong)
    {
        this.k = paramLong;
        this.m = true;
    }

    public final void c()
    {
        if ((!this.m) && (this.k == 9223372036854775807L))
            this.k = System.currentTimeMillis();
    }

    public final void a(Location paramLocation)
    {
        this.q = new double[] { paramLocation.getLatitude(), paramLocation.getLongitude() };
    }

    public final String toString()
    {
        String str = "";
        str = str + "URI            : " + this.i + "\n";
     //   str = str + "URI Builder    : " + this.h.toString() + "\n";
        str = str + "\n";
        //str = str + "Logged by      : " + this.c.toString() + "\n";
       // str = str + "Error:         : " + this.g + "\n";
        str = str + "\n";
        str = str + "Response time  : " + f() + "\n";
        str = str + "Start time     : " + this.a + "\n";
        str = str + "End time       : " + this.k + "\n";
        str = str + "\n";
        str = str + "Bytes out    : " + this.d + "\n";
        str = str + "Bytes in     : " + this.n + "\n";
        str = str + "\n";
        str = str + "Response code  : " + this.e + "\n";
        str = str + "Request method : " + this.f + "\n";
        if (this.q != null)
            str = str + "Location       : " + Arrays.toString(this.q) + "\n";
        return str;
    }

    public final JSONArray d()
    {
        JSONArray localJSONArray1 = new JSONArray();
        try
        {
            localJSONArray1.put(this.f);
            localJSONArray1.put(a());
           // localJSONArray1.put(dr.a.a(new Date(this.a)));
            localJSONArray1.put(f());
           // localJSONArray1.put(this.j.a());
            localJSONArray1.put(this.n);
            localJSONArray1.put(this.d);
            localJSONArray1.put(this.e);
            localJSONArray1.put(3);
         //   localJSONArray1.put(Integer.toString(this.g.a()));
            if (this.q != null)
            {
                JSONArray localJSONArray2;
                (localJSONArray2 = new JSONArray()).put(this.q[0]);
                localJSONArray2.put(this.q[1]);
                localJSONArray1.put(localJSONArray2);
            }
        }
        catch (Exception localException)
        {
            System.out.println("Failed to create statsArray");
            localJSONArray1 = null;
            localException.printStackTrace();
        }
        return localJSONArray1;
    }

    public final void a(InetAddress paramInetAddress)
    {
        this.i = null;
     //   this.h.a = paramInetAddress;
    }

    public final void a(String paramString)
    {
        this.i = null;
      //  this.h.MeasuredRequest = paramString;
    }

    public final void a(Object parama)
    {
       // this.h.d = parama;
    }

    public final void a(int paramInt)
    {
        int i1 = paramInt;

     /*   if (i1 > 0)
            paramInt.e = i1;*/
    }

    public final void a(OutputStream paramOutputStream)
    {
        JSONArray localJSONArray = d();
        OutputStreamWriter paramOutputStreamWriter;
        try{
            (paramOutputStreamWriter = new OutputStreamWriter(paramOutputStream)).write(localJSONArray.toString());
            paramOutputStreamWriter.close();
        }catch (Exception e){

        }
    }

    public final void e()
    {
   //     this.h.f = true;
    }

    public static class a
    {
        private String l;

        private a(String arg3)
        {
          /*  Object localObject;
            this.l = localObject;*/
        }

        public final String toString()
        {
            return this.l;
        }
    }
}