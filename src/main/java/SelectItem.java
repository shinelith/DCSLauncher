import java.util.Objects;

public class SelectItem {
    private String text;
    private Object extra;
    public SelectItem(String text, Object obj) {
        this.text = text;
        this.extra = obj;
    }
    public String getText() {
        return text;
    }
    public Object getExtra() {
        return extra;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectItem that = (SelectItem) o;
        return Objects.equals(extra, that.extra);
//        if(o instanceof SelectItem){
//            SelectItem that = (SelectItem) o;
//            return text.equals(that.text);
//        }else{
//            return false;
//        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, extra);
    }

}
