package net.fjos.annweather;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Kim on 01.04.2016.
 */

//dette er weatheradapteret, den tar seg av listView og håndteringen av objektene.
public class WeatherAdapter extends ArrayAdapter<Weather> {

    Context context;
    int layoutResourceId;
    List<Weather> data;


//her settes context, layout og listen.
    public WeatherAdapter(Context context, int layoutResourceId, List<Weather> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    //her blir objektene satt, deretter settes objektene opp mot Textview og Imageview.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        WeatherHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new WeatherHolder();
            holder.imgIcon = (ImageView)row.findViewById(R.id.imgIcon);
            holder.txtTitle = (TextView)row.findViewById(R.id.txtTitle);
            holder.txtTime = (TextView)row.findViewById(R.id.txtTime);
            holder.txtTemp = (TextView)row.findViewById(R.id.txtTemp);
            holder.txtVind = (TextView)row.findViewById(R.id.txtVind);

            row.setTag(holder);
        }
        else
        {
            holder = (WeatherHolder)row.getTag();
        }
        Weather weather = data.get(position);
        Context context = holder.imgIcon.getContext();
        int id = context.getResources().getIdentifier(weather.icon, "drawable", context.getPackageName());;
        holder.txtTitle.setText(weather.title);
        holder.txtTime.setText(weather.tid);
        holder.txtTemp.setText(weather.temperatur);
        holder.txtVind.setText(weather.vind);
        holder.imgIcon.setImageResource(id);

        return row;
    }

    static class WeatherHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
        TextView txtTime;
        TextView txtVind;
        TextView txtTemp;
    }
}