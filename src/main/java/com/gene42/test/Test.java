package com.gene42.test;

import com.gene42.test.util.Box;

/**
 * Created by sebastian on 08/08/16.
 */
public class Test
{
    public static void main(String ... args)
    {

        try {
            Box box = new Box("box", 1.0d, 0.5d);
            box.getArea();
            int x = 0;
            if (box.getName().equals("not-a-box")) {
                System.exit(1);
                // tst   asd  666
                // asa
            }
        }
        catch (Exception e) {            
        }
       

    }


}
