package com.mparticle.networking;

import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;

public final class y extends Provider.Service
{
  private Provider.Service d;
  public static final String[] a = { "Default", "SSL", "TLSv1.1", "TLSv1.2", "SSLv3", "TLSv1", "TLS" };

  private y(Provider.Service paramService)
  {
    super(paramService.getProvider(), paramService.getType(), paramService.getAlgorithm(), paramService.getClassName(), null, null);
    this.d = paramService;
  }

  private static y a(Provider.Service paramService)
  {
      y service = new y(paramService);
    try
    {

        Field[] fields = Service.class.getFields();
      for (int i = 0; i < fields.length; i++)
      {
          fields[i].setAccessible(true);
          fields[i].set(service, fields[i].get(service));
      }
    }
    catch (Exception localException)
    {
      return null;
    }
    return service;
  }

  public final Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
    return d.newInstance(constructorParameter);
  }
}