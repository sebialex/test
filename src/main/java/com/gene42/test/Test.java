package com.gene42.test;

import com.gene42.test.util.Box;

/**
 * Created by sebastian on 08/08/16.
 */
public class Test
{
    public static void main(String ... args) {

        Box box = new Box("box", 1.0d, 0.5d);
        box.getArea();

        if (box.getName().equals("not-a-box")) {
            System.exit(1);
        }

    }


}
