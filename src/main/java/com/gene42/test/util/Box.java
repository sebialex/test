package com.gene42.test.util;

import java.util.Date;

/**
 * Created by sebastian on 08/08/16.
 */
public class Box
{

    private Date creationDate;

    private double width;
    private double height;


    public Box(double width, double height){
        this.width = width;
        this.creationDate = new Date();
    }

    /**
     * Getter for creationDate
     *
     * @return creationDate
     */
    public Date getCreationDate()
    {
        return creationDate;
    }

    /**
     * Setter for creationDate.
     *
     * @ param creationDate
     */
    public void setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
    }

    /**
     * Getter for width
     *
     * @return width
     */
    public double getWidth()
    {
        return width;
    }

    /**
     * Setter for width.
     *
     * @ param width
     */
    public void setWidth(double width)
    {
        this.width = width;
    }

    /**
     * Getter for height
     *
     * @return height
     */
    public double getHeight()
    {
        return height;
    }

    /**
     * Setter for height.
     *
     * @ param height
     */
    public void setHeight(double height)
    {
        this.height = height;
    }
}
