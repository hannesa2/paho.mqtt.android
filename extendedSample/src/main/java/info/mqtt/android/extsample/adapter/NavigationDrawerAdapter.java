package info.mqtt.android.extsample.adapter;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.mqtt.android.extsample.R;
import info.mqtt.android.extsample.model.NavDrawerItem;

import java.util.Collections;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.MyViewHolder> {
    private final LayoutInflater inflater;
    private final Context context;
    private List<NavDrawerItem> data = Collections.emptyList();

    public NavigationDrawerAdapter(Context context, List<NavDrawerItem> data) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.data = data;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.nav_drawer_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        NavDrawerItem current = data.get(position);
        holder.navTitle.setText(current.getTitle());
        Drawable doneCloud = ContextCompat.getDrawable(context, R.drawable.ic_cloud_done_dark);
        holder.icon.setImageDrawable(doneCloud);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView navTitle;
        final ImageView icon;

        public MyViewHolder(View itemView) {
            super(itemView);
            navTitle = itemView.findViewById(R.id.navTitle);
            icon = itemView.findViewById(R.id.connection_icon);
        }
    }
}