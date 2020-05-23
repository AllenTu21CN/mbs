package mbs.studio.view;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.studio.R;

public class SimpleArrayAdapter<T> extends ArrayAdapter<T> {

    private static final int VIEW_RES_ID = android.R.layout.simple_spinner_item;
    private static final int DROP_DOWN_VIEW_RES_ID = android.R.layout.simple_spinner_dropdown_item;
    private static final int mFieldId = 0;

    private LayoutInflater mInflater;
    private float mTextSize = 28;

    public SimpleArrayAdapter(Context context, T[] objects) {
        super(context, 0, objects);

        mInflater = LayoutInflater.from(context);
    }

    public void setTextSize(float size) {
        mTextSize = size;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(mInflater, position, convertView, parent, DROP_DOWN_VIEW_RES_ID);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(mInflater, position, convertView, parent, VIEW_RES_ID);
    }

    private @NonNull View createViewFromResource(@NonNull LayoutInflater inflater, int position,
                                                 @Nullable View convertView, @NonNull ViewGroup parent, int resource) {
        final View view;
        final TextView text;

        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        try {
            if (mFieldId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView) view;
            } else {
                //  Otherwise, find the TextView field within the layout
                text = view.findViewById(mFieldId);

                if (text == null) {
                    throw new RuntimeException("Failed to find view with ID "
                            + getContext().getResources().getResourceName(mFieldId)
                            + " in item layout");
                }
            }
        } catch (ClassCastException e) {
            Log.e("SimpleArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        final T item = getItem(position);
        if (item instanceof CharSequence) {
            text.setText((CharSequence) item);
        } else {
            text.setText(item.toString());
        }

        text.setTextColor(getContext().getColor(R.color.primaryTextColor));
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        text.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        Utils.setSize(text, ViewGroup.LayoutParams.WRAP_CONTENT, (int)mTextSize * 2);

        return view;
    }
}
