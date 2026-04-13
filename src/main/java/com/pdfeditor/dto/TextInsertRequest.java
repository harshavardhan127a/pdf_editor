package com.pdfeditor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class TextInsertRequest {

    @Min(value = 0, message = "Page number must be >= 0")
    private int pageNumber;

    @NotBlank(message = "Text content is required")
    private String text;

    private float x = 100;
    private float y = 700;
    private float fontSize = 12;
    private String fontName = "Helvetica";
    private String color = "#000000";

    public TextInsertRequest() {}

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }
    public String getFontName() { return fontName; }
    public void setFontName(String fontName) { this.fontName = fontName; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
