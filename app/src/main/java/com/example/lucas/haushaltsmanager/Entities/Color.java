package com.example.lucas.haushaltsmanager.Entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;

import java.util.Random;
import java.util.regex.Pattern;

public class Color extends android.graphics.Color implements Parcelable {
    public static final Creator<Color> CREATOR = new Creator<Color>() {

        @Override
        public Color createFromParcel(Parcel in) {

            return new Color(in);
        }

        @Override
        public Color[] newArray(int size) {

            return new Color[size];
        }
    };
    static final String VALID_COLOR_PATTERN = "^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$";
    private final String color;

    public Color(String color) {
        guardAgainstInvalidColorString(color);

        this.color = color;
    }

    public Color(@ColorInt int color) {
        this.color = "#" + Integer.toHexString(color);
    }

    private Color(Parcel source) {
        color = source.readString();
    }

    public static Color black() {
        return new Color("#000000");
    }

    public static Color white() {
        return new Color("#FFFFFF");
    }

    public static Color random() {
        Random random = new Random();

        int nextInt = random.nextInt(0xffffff + 1);

        return new Color(String.format("#%06x", nextInt));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Color)) {
            return false;
        }

        Color other = (Color) o;

        return other.color.equals(color);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(color);
    }

    public String getColorString() {
        return color;
    }

    @ColorInt
    public int getColorInt() {
        return parseColor(color);
    }

    private void guardAgainstInvalidColorString(String colorString) {
        if (assertIsColorString(colorString)) {
            return;
        }

        throw new IllegalArgumentException(String.format("Could not create Color from: '%s'", colorString));
    }

    // TODO: Könnte getBrightness Funktion enthalten

    private boolean assertIsColorString(String color) {
        return Pattern.compile(VALID_COLOR_PATTERN)
                .matcher(color)
                .matches();
    }
}
