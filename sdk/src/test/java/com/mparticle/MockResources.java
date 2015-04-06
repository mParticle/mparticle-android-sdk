package com.mparticle;

/**
 * Created by sdozor on 3/23/15.
 */
public class MockResources extends android.test.mock.MockResources {
    public static String TEST_APP_KEY = "the app key";
    public static String TEST_APP_SECRET = "the app secret";

    @Override
    public int getIdentifier(String name, String defType, String defPackage) {
        if (name.equals("mp_key")){
            return 1;
        }else if (name.equals("mp_secret")){
            return 2;
        }

        return 0;
    }

    @Override
    public String getString(int id) throws NotFoundException {
        switch (id){
            case 1:
                return TEST_APP_KEY;
            case 2:
                return TEST_APP_SECRET;

        }
        return null;
    }

    @Override
    public String getString(int id, Object... formatArgs) throws NotFoundException {
        return super.getString(id, formatArgs);
    }
}
