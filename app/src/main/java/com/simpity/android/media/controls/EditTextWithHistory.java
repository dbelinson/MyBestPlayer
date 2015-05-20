package com.simpity.android.media.controls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.simpity.android.media.Res;

public class EditTextWithHistory extends LinearLayout implements 
		View.OnClickListener, TextWatcher {

	private ImageButton mButton;
	private AutoCompleteTextView mTextEdit;

	private String mHistoryKey;
	private Vector<String> mHistory;
	private List<String> mAutoComplete;
	private ArrayAdapter<String> mAutoCompleteAdapter;
	
	private TextChangeListener mListener = null;
	private String mCurrentText;

	public interface TextChangeListener {
		public void onEditTextChanged(String new_text);
	};
	
	//--------------------------------------------------------------------------
	public EditTextWithHistory(Context context) {
		super(context);
		init(context);
	}

	//--------------------------------------------------------------------------
	public EditTextWithHistory(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);

		TypedArray a = context.obtainStyledAttributes(attrs, Res.styleable.EditTextWithHistory);
		setHistoryKey(a.getString(Res.styleable.EditTextWithHistory_historyKey));
		a.recycle();
	}

	//--------------------------------------------------------------------------
	private void init(Context context) {
		View view = LayoutInflater.from(context).inflate(Res.layout.history_text_edit, null);
		addView(view);

		mButton = (ImageButton)view.findViewById(Res.id.HistoryTextEditButton);
		mButton.setOnClickListener(this);
		mButton.setEnabled(false);

		mTextEdit = (AutoCompleteTextView)view.findViewById(Res.id.HistoryTextEdit);
		mTextEdit.addTextChangedListener(this);
	}

	//--------------------------------------------------------------------------
	@Override
	public android.os.Parcelable onSaveInstanceState() {
		android.os.Parcelable superState = super.onSaveInstanceState();

		SavedState state = new SavedState(superState);
		state.enteredText = mTextEdit.getText().toString();
		if (HistoryDialog != null ) {
			state.isDialogShowing = HistoryDialog.isShowing();
			if (state.isDialogShowing)
				HistoryDialog.dismiss();
		}
		return state;
	};

	//--------------------------------------------------------------------------
	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (state == null || !(state instanceof SavedState)) {
            try {
            	super.onRestoreInstanceState(state);
            } catch (Exception e) {
            	e.printStackTrace();
			}
            return;
        }
		SavedState myState = (SavedState) state;
		try {
			super.onRestoreInstanceState(myState.getSuperState());
		} catch (Exception e) {
			e.printStackTrace();
		}
        mTextEdit.setText(myState.enteredText);
        if (myState.isDialogShowing) {
        	showHistoryDialog();
        }
	};

	//--------------------------------------------------------------------------
	public void selectAll() {
		if (mTextEdit != null) {
			mTextEdit.selectAll();
		}
	}
	
	//--------------------------------------------------------------------------
	public void setHistoryKey(String key) {
		mHistoryKey = key;
		loadHistory();

		if (mHistory != null) {
			if (mButton != null)
				mButton.setEnabled(true);

			if (mTextEdit != null && mAutoComplete == null) {
				String[] history = new String[mHistory.size()];
				mHistory.toArray(history);
				mAutoCompleteAdapter = new ArrayAdapter<String>(getContext(),
						android.R.layout.simple_dropdown_item_1line, history);
				mTextEdit.setAdapter(mAutoCompleteAdapter);
			}
		} else {
			if (mButton != null)
				mButton.setEnabled(false);
		}
	}

	//--------------------------------------------------------------------------
	public void reloadHistory() {
		loadHistory();
	}

	//--------------------------------------------------------------------------
	private void loadHistory() {
		if (mHistoryKey != null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
			String values = prefs.getString(mHistoryKey, null);
			if (values != null) {
				BufferedReader reader = new BufferedReader(new StringReader(values));
				mHistory = new Vector<String>();

				try {
					String line = reader.readLine();
					while (line != null) {
						mHistory.add(line);
						line = reader.readLine();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				mHistory = null;
			}
		}
	}

	//--------------------------------------------------------------------------
	public void storeHistory() {
		if (mHistoryKey == null)
			return;

		String value = getText();

		if (value.length() == 0)
			return;

		if (mHistory != null) {
			boolean exist = false;
			for (String str : mHistory)
				if (str.compareTo(value) == 0) {
					exist = true;
					mHistory.remove(str);
					mHistory.insertElementAt(value, 0);
					break;
				}

			if (!exist) {
				mHistory.insertElementAt(value, 0);
			}
		} else {
			mHistory = new Vector<String>();
			mHistory.add(value);
		}

		StringBuilder builder = new StringBuilder();
		for (String str : mHistory) {
			builder.append(str);
			builder.append('\n');
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		SharedPreferences.Editor edit = prefs.edit();

		edit.putString(mHistoryKey, builder.toString());
		edit.commit();
	}

	//--------------------------------------------------------------------------
	public void setText(String text) {
		mTextEdit.setText(text);
		mCurrentText = text;
	}

	//--------------------------------------------------------------------------
	public String getText() {
		return mTextEdit.getEditableText().toString();
	}

	//--------------------------------------------------------------------------
	public void setAutoComplete(List<String> auto_complete) {
		mAutoComplete = auto_complete;

		List<String> items;

		if (mAutoComplete != null) {
			items = mAutoComplete;
		} else if (mHistory != null) {
			items = mHistory;
		} else {
			items = null;
		}

		if (items != null) {
			mAutoCompleteAdapter = new ArrayAdapter<String>(getContext(),
					android.R.layout.simple_dropdown_item_1line, items);
			mTextEdit.setAdapter(mAutoCompleteAdapter);
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void onClick(View v) {

		showHistoryDialog();
	}

	//--------------------------------------------------------------------------
	private void showHistoryDialog() {
		if (mHistory != null) {
			Context context = getContext();

			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setCancelable(true);

			String[] history = new String[mHistory.size()];
			mHistory.toArray(history);
			dialog.setItems(history, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					HistoryDialog = null;
					mTextEdit.setText(mHistory.get(which));
				}
			});
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					HistoryDialog = null;
				}
			});

			HistoryDialog = dialog.show();
		}
	}

	//--------------------------------------------------------------------------
	private AlertDialog HistoryDialog = null;

	private static class SavedState extends BaseSavedState {
        boolean isDialogShowing = false;
        String enteredText = null; 

        public SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            enteredText = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeString(enteredText);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }
    }

	//--------------------------------------------------------------------------
	public void setEditTextChangeListener(TextChangeListener listener) {
		mListener = listener;
		if (listener != null) {
			mCurrentText = mTextEdit.getText().toString();
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void afterTextChanged(Editable s) {
		if (mListener != null) {
			String text = mTextEdit.getText().toString();
			if (mCurrentText == null || !mCurrentText.equals(text)) {
				mListener.onEditTextChanged(text);
				mCurrentText = text;
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	//--------------------------------------------------------------------------
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}
	
	//--------------------------------------------------------------------------
	public void setUrlInputType() {
		if (mTextEdit != null) {
			mTextEdit.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
		}
	}
	
	//--------------------------------------------------------------------------
	public void setOnEditorActionListener(TextView.OnEditorActionListener listener) {
		if (mTextEdit != null) {
			mTextEdit.setOnEditorActionListener(listener);
			//mTextEdit.setImeActionLabel(getContext().getString(text_id), text_id);			
		}
	}
}
