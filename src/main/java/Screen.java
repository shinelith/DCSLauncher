import java.util.Objects;

public class Screen {
    private int width;
    private int height;

    public Screen(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Screen screen = (Screen) o;
        return width == screen.width &&
                height == screen.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    public float getAspect() {
        return (float) width / (float) height;
    }
}
