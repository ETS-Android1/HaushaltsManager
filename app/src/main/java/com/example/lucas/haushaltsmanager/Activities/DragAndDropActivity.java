package com.example.lucas.haushaltsmanager.Activities;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lucas.haushaltsmanager.R;
import com.example.lucas.haushaltsmanager.RecyclerView.AdditionalFunctionality.InsertStrategy.AppendInsertStrategy;
import com.example.lucas.haushaltsmanager.RecyclerView.Items.CardViewItem.CardViewItem;
import com.example.lucas.haushaltsmanager.RecyclerView.Items.IRecyclerItem;
import com.example.lucas.haushaltsmanager.RecyclerView.ListAdapter.CardViewRecyclerViewAdapter;
import com.example.lucas.haushaltsmanager.ReportBuilder.PieChart;

import java.util.ArrayList;
import java.util.List;

public class DragAndDropActivity extends AbstractAppCompatActivity implements View.OnDragListener {
    private RecyclerView rView;
    private CardView cView;
    private ConstraintLayout cLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drag_and_drop);

        rView = findViewById(R.id.recycler_view);
        rView.setOnDragListener(this);
        setUpRecyclerView();


        cView = findViewById(R.id.card_view);

        cLayout = findViewById(R.id.card_view_root);
        cLayout.setOnDragListener(this);
    }

    @Override
    public boolean onDrag(View targetView, DragEvent event) {
        // Defines a variable to store the action type for the incoming event
        int action = event.getAction();
        // Handles each of the expected events
        switch (action) {

            case DragEvent.ACTION_DRAG_STARTED:
                // Determines if this View can accept the dragged data
                if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    // if you want to apply color when drag started to your view you can uncomment below lines
                    // to give any color tint to the View to indicate that it can accept data.
                    // v.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
                    // Invalidate the view to force a redraw in the new tint
                    //  v.invalidate();
                    // returns true to indicate that the View can accept the dragged data.
                    return true;
                }
                // Returns false. During the current drag and drop operation, this View will
                // not receive events again until ACTION_DRAG_ENDED is sent.
                return false;

            case DragEvent.ACTION_DRAG_ENTERED:
                // Applies a GRAY or any color tint to the View. Return true; the return value is ignored.
                targetView.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                // Invalidate the view to force a redraw in the new tint
                targetView.invalidate();
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                // Ignore the event
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                // Re-sets the color tint to blue. Returns true; the return value is ignored.
                // view.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
                //It will clear a color filter .
                targetView.getBackground().clearColorFilter();
                // Invalidate the view to force a redraw in the new tint
                targetView.invalidate();
                return true;

            case DragEvent.ACTION_DROP:
                // Gets the item containing the dragged data
                ClipData.Item item = event.getClipData().getItemAt(0);
                // Gets the text data from the item.
                String dragData = item.getText().toString();
                // Displays a message containing the dragged data.
                Toast.makeText(this, "Dragged data is " + dragData, Toast.LENGTH_SHORT).show();
                // Turns off any color tints
                targetView.getBackground().clearColorFilter();
                // Invalidates the view to force a redraw
                targetView.invalidate();

                View vw = (View) event.getLocalState();
                ViewGroup owner = (ViewGroup) vw.getParent();
                owner.removeView(vw); //remove the dragged view
                //caste the view into LinearLayout as our drag acceptable layout is LinearLayout
                ConstraintLayout container = (ConstraintLayout) targetView;
                container.addView(vw);//Add the dragged view
                vw.setVisibility(View.VISIBLE);//finally set Visibility to VISIBLE
                // Returns true. DragEvent.getResult() will return true.
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                // Turns off any color tinting
                targetView.getBackground().clearColorFilter();
                // Invalidates the view to force a redraw
                targetView.invalidate();
                // Does a getResult(), and displays what happened.
                if (event.getResult())
                    Toast.makeText(this, "The drop was handled.", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "The drop didn't work.", Toast.LENGTH_SHORT).show();
                // returns true; the value is ignored.
                return true;
            // An unknown action type was received.
            default:
                Log.e("DragDrop Example", "Unknown action type received by OnDragListener.");
                break;
        }
        return false;
    }

    private void setUpRecyclerView() {
        rView.setLayoutManager(new LinearLayoutManager(this));

        List<IRecyclerItem> items = new ArrayList<>();
        items.add(new CardViewItem(new PieChart(this)));
        items.add(new CardViewItem(new PieChart(this)));
        items.add(new CardViewItem(new PieChart(this)));
        items.add(new CardViewItem(new PieChart(this)));
        items.add(new CardViewItem(new PieChart(this)));
        items.add(new CardViewItem(new PieChart(this)));
        items.add(new CardViewItem(new PieChart(this)));
        CardViewRecyclerViewAdapter adapter = new CardViewRecyclerViewAdapter(items, new AppendInsertStrategy());
        rView.setAdapter(adapter);
    }
}
