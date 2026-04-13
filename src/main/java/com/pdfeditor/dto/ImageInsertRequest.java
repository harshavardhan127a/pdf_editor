package com.pdfeditor.dto;

import jakarta.validation.constraints.Min;

public class ImageInsertRequest {

    @Min(value = 0, message = "Page number must be >= 0")
    private int pageNumber;

    private float x = 100;
    private float y = 500;
    private float width = 200;
    private float height = 200;

    public ImageInsertRequest() {}

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
}
