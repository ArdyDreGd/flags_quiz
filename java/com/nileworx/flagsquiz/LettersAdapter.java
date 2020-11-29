package com.nileworx.flagsquiz;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class LettersAdapter extends BaseAdapter {
	private Activity activity;
	protected ArrayList<HashMap<String, String>> data;
	private static LayoutInflater inflater = null;
	Context context;
	Typeface tf;

	public LettersAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
		activity = a;
		data = d;
		context = a;
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return data.size();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if (convertView == null)
			vi = inflater.inflate(R.layout.letter_row_grid, null);
		TextView letterBall = (TextView) vi.findViewById(R.id.letterButton);
		HashMap<String, String> ballMap = new HashMap<String, String>();
		ballMap = data.get(position);
		letterBall.setText(ballMap.get(GameActivity.KEY_LETTER_GAME).toUpperCase());
		return vi;
	}

}