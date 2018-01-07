/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.listeners.AudioPlayListener;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.utilities.TextUtils;
import org.odk.collect.android.views.MediaLayout;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public abstract class QuestionWidget extends RelativeLayout implements AudioPlayListener {

    @SuppressWarnings("unused")
    private static final String t = "QuestionWidget";
    private static int idGenerator = 1211322;

    /**
     * Generate a unique ID to keep Android UI happy when the screen orientation
     * changes.
     */
    public static int newUniqueId() {
        return ++idGenerator;
    }

    protected FormEntryPrompt mPrompt;
    protected MediaLayout mQuestionMediaLayout;

    protected final int mQuestionFontsize;
    protected final int mAnswerFontsize;

    private TextView mHelpTextView;
    TextView questionText;

    protected MediaPlayer mPlayer;

    protected int mPlayColor = Color.BLUE;
    protected int mPlayBackgroundColor = Color.WHITE;

    public QuestionWidget(Context context, FormEntryPrompt p) {
        super(context);

        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mQuestionMediaLayout.resetTextFormatting();
                mediaPlayer.reset();
            }

        });

        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Timber.e("Error occured in MediaPlayer. what = %d, extra = %d",
                        what, extra);
                return false;
            }
        });

        mQuestionFontsize = Collect.getQuestionFontsize();
        //sorry we modify you :( its too large
        mAnswerFontsize = mQuestionFontsize - 1;

        mPrompt = p;

        setGravity(Gravity.TOP);
        setPadding(0, 7, 0, 0);

        mQuestionMediaLayout = createQuestionMediaLayout(p);
        mHelpTextView = createHelpText(p);

        addQuestionMediaLayout(mQuestionMediaLayout);
        addHelpTextView(mHelpTextView);
    }

    private MediaLayout createQuestionMediaLayout(FormEntryPrompt p) {
        String imageURI = p.getImageText();
        String audioURI = p.getAudioText();
        String videoURI = p.getSpecialFormQuestionText("video");

        // shown when image is clicked
        String bigImageURI = p.getSpecialFormQuestionText("big-image");

        String promptText = p.getLongText();
        // Add the text view. Textview always exists, regardless of whether there's text.
        questionText = new TextView(getContext());
        questionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mQuestionFontsize);
        questionText.setTypeface(null, Typeface.BOLD);
        questionText.setPadding(0, 0, 0, 7);
        questionText.setText(promptText == null ? "" : TextUtils.textToHtml(promptText));
        questionText.setMovementMethod(LinkMovementMethod.getInstance());

        // Wrap to the size of the parent view
        questionText.setHorizontallyScrolling(false);

        if (promptText == null || promptText.length() == 0) {
            questionText.setVisibility(GONE);
        }

        // Create the layout for audio, image, text
        MediaLayout questionMediaLayout = new MediaLayout(getContext(), mPlayer);
        questionMediaLayout.setId(QuestionWidget.newUniqueId()); // assign random id
        questionMediaLayout.setAVT(p.getIndex(), "", questionText, audioURI, imageURI, videoURI,
                bigImageURI);
        questionMediaLayout.setAudioListener(this);

        String playColorString = p.getFormElement().getAdditionalAttribute(null, "playColor");
        if (playColorString != null) {
            try {
                mPlayColor = Color.parseColor(playColorString);
            } catch (IllegalArgumentException e) {
                Timber.e(e, "Argument %s is incorrect", playColorString);
            }
        }
        questionMediaLayout.setPlayTextColor(mPlayColor);

        String playBackgroundColorString = p.getFormElement().getAdditionalAttribute(null,
                "playBackgroundColor");
        if (playBackgroundColorString != null) {
            try {
                mPlayBackgroundColor = Color.parseColor(playBackgroundColorString);
            } catch (IllegalArgumentException e) {
                Timber.e(e, "Argument %s is incorrect", playBackgroundColorString);
            }
        }
        questionMediaLayout.setPlayTextBackgroundColor(mPlayBackgroundColor);

        return questionMediaLayout;
    }

    public MediaLayout getQuestionMediaLayout() {
        return mQuestionMediaLayout;
    }

    public TextView getHelpTextView() {
        return mHelpTextView;
    }

    public void playAudio() {
        playAllPromptText();
    }

    public void playVideo() {
        mQuestionMediaLayout.playVideo();
    }

    public FormEntryPrompt getPrompt() {
        return mPrompt;
    }

    public MediaLayout getQuestionMediaView() {
        return mQuestionMediaLayout;
    }

    // http://code.google.com/p/android/issues/detail?id=8488
    private void recycleDrawablesRecursive(ViewGroup viewGroup, List<ImageView> images) {

        int childCount = viewGroup.getChildCount();
        for (int index = 0; index < childCount; index++) {
            View child = viewGroup.getChildAt(index);
            if (child instanceof ImageView) {
                images.add((ImageView) child);
            } else if (child instanceof ViewGroup) {
                recycleDrawablesRecursive((ViewGroup) child, images);
            }
        }
        viewGroup.destroyDrawingCache();
    }

    // http://code.google.com/p/android/issues/detail?id=8488
    public void recycleDrawables() {
        List<ImageView> images = new ArrayList<ImageView>();
        // collect all the image views
        recycleDrawablesRecursive(this, images);
        for (ImageView imageView : images) {
            imageView.destroyDrawingCache();
            Drawable d = imageView.getDrawable();
            if (d != null && d instanceof BitmapDrawable) {
                imageView.setImageDrawable(null);
                BitmapDrawable bd = (BitmapDrawable) d;
                Bitmap bmp = bd.getBitmap();
                if (bmp != null) {
                    bmp.recycle();
                }
            }
        }
    }

    // Abstract methods
    public abstract IAnswerData getAnswer();

    public abstract void clearAnswer();

    public abstract void setFocus(Context context);

    public abstract void setOnLongClickListener(OnLongClickListener l);

    /**
     * Override this to implement fling gesture suppression (e.g. for embedded WebView treatments).
     *
     * @return true if the fling gesture should be suppressed
     */
    public boolean suppressFlingGesture(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        return false;
    }

    /**
     * Add a Views containing the question text, audio (if applicable), and image (if applicable).
     * To satisfy the RelativeLayout constraints, we add the audio first if it exists, then the
     * TextView to fit the rest of the space, then the image if applicable.
     */

    /**
     * Defaults to adding questionlayout to the top of the screen.
     * Overwrite to reposition.
     */
    protected void addQuestionMediaLayout(View v) {
        if (v == null) {
            Timber.e("cannot add a null view as questionMediaLayout");
            return;
        }
        // default for questionmedialayout
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.setMargins(10, 0, 10, 0);
        addView(v, params);
    }




    /**
     * Add a TextView containing the help text to the default location.
     * Override to reposition.
     */
    protected void addHelpTextView(View v) {
        if (v == null) {
            Timber.e("cannot add a null view as helpTextView");
            return;
        }

        // default for helptext
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.BELOW, mQuestionMediaLayout.getId());
        params.setMargins(10, 0, 10, 0);
        addView(v, params);
    }

    private TextView createHelpText(FormEntryPrompt p) {
        TextView helpText = new TextView(getContext());
        String s = p.getHelpText();

        if (s != null && !s.equals("")) {
            helpText.setId(QuestionWidget.newUniqueId());
            helpText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mQuestionFontsize - 3);
            helpText.setPadding(0, -5, 0, 7);
            // wrap to the widget of view
            helpText.setHorizontallyScrolling(false);
            helpText.setTypeface(null, Typeface.ITALIC);
            helpText.setText(TextUtils.textToHtml(s));
            helpText.setMovementMethod(LinkMovementMethod.getInstance());
            return helpText;
        } else {
            helpText.setVisibility(View.GONE);
            return helpText;
        }
    }

    /**
     * Default place to put the answer
     * (below the help text or question text if there is no help text)
     * If you have many elements, use this first
     * and use the standard addView(view, params) to place the rest
     */
    protected void addAnswerView(View v) {
        if (v == null) {
            Timber.e("cannot add a null view as an answerView");
            return;
        }
        // default place to add answer
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        if (mHelpTextView.getVisibility() == View.VISIBLE) {
            params.addRule(RelativeLayout.BELOW, mHelpTextView.getId());
        } else {
            params.addRule(RelativeLayout.BELOW, mQuestionMediaLayout.getId());
        }
        params.setMargins(10, 0, 10, 0);
        addView(v, params);
    }

    /**
     * Every subclassed widget should override this, adding any views they may contain, and calling
     * super.cancelLongPress()
     */
    public void cancelLongPress() {
        super.cancelLongPress();
        if (mQuestionMediaLayout != null) {
            mQuestionMediaLayout.cancelLongPress();
        }
        if (mHelpTextView != null) {
            mHelpTextView.cancelLongPress();
        }
    }

    /*
     * Prompts with items must override this
     */
    public void playAllPromptText() {
        mQuestionMediaLayout.playAudio();
    }

    public void setQuestionTextColor(int color) {
        mQuestionMediaLayout.setTextcolor(color);
    }

    public void resetQuestionTextColor() {
        mQuestionMediaLayout.resetTextFormatting();
    }

    public void hideQuestion(){
        questionText.setVisibility(GONE);
        mHelpTextView.setVisibility(GONE);
        mQuestionMediaLayout.setVisibility(GONE);
    }

    public void setQuestionParams(RelativeLayout.LayoutParams params){
        questionText.setLayoutParams(params);
        mHelpTextView.setLayoutParams(params);
        mQuestionMediaLayout.setLayoutParams(params);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility == INVISIBLE || visibility == GONE) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
                mPlayer.reset();
            }
        }
    }

    public void stopAudio() {
        if (mPlayer.isPlaying()) {
            mPlayer.stop();
            mPlayer.reset();
        }
    }

    /**
     * It's needed only for external choices. Everything works well and
     * out of the box when we use internal choices instead
     */
    protected void clearNextLevelsOfCascadingSelect() {
        FormController formController = Collect.getInstance().getFormController();
        if (formController.currentCaptionPromptIsQuestion()) {
            try {
                FormIndex startFormIndex = formController.getQuestionPrompt().getIndex();
                formController.stepToNextScreenEvent();
                while (formController.currentCaptionPromptIsQuestion()
                        && formController.getQuestionPrompt().getFormElement().getAdditionalAttribute(null, "query") != null) {
                    formController.saveAnswer(formController.getQuestionPrompt().getIndex(), null);
                    formController.stepToNextScreenEvent();
                }
                formController.jumpToIndex(startFormIndex);
            } catch (JavaRosaException e) {
                Timber.e(e);
            }
        }
    }

}
