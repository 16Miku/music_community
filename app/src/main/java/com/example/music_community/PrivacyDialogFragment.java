package com.example.music_community;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

public class PrivacyDialogFragment extends DialogFragment {

    public interface PrivacyDialogListener {
        void onPrivacyAgreed();
        void onPrivacyDisagreed();
    }

    private PrivacyDialogListener listener;

    public PrivacyDialogFragment() {
        // DialogFragment 的无参构造函数是必须的
    }

    @Override
    public void onAttach(@NonNull Context context) {

        super.onAttach(context);

        if (context instanceof PrivacyDialogListener) {

            listener = (PrivacyDialogListener) context;

        } else {

            throw new RuntimeException(context.toString()
                    + " must implement PrivacyDialogListener");

        }
    }

    @Override
    public void onDetach() {

        super.onDetach();

        listener = null;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_Music_community_PrivacyDialog);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_privacy_dialog, container, false);

        Button agreeButton = view.findViewById(R.id.btn_agree);

        TextView disagreeButton = view.findViewById(R.id.btn_disagree);

        TextView privacyContentTv = view.findViewById(R.id.privacy_content);

        agreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {

                    listener.onPrivacyAgreed();

                }

                dismiss();
            }
        });

        disagreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {

                    listener.onPrivacyDisagreed();

                }
                dismiss();
            }
        });

        String fullContent = getString(R.string.privacy_dialog_content);

        SpannableString spannableString = new SpannableString(fullContent);

        String userAgreementText = getString(R.string.user_agreement_text);

        int startIndexUser = fullContent.indexOf(userAgreementText);

        if (startIndexUser != -1) {

            int endIndexUser = startIndexUser + userAgreementText.length();

            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {

                    openBrowser("https://www.mi.com");

                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);


                    ds.setColor(ContextCompat.getColor(getContext(), R.color.link_blue));


                    ds.setUnderlineText(false);
                }

            }, startIndexUser, endIndexUser, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        String privacyPolicyText = getString(R.string.privacy_policy_text);

        int startIndexPrivacy = fullContent.indexOf(privacyPolicyText);

        if (startIndexPrivacy != -1) {

            int endIndexPrivacy = startIndexPrivacy + privacyPolicyText.length();

            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {

                    openBrowser("https://www.xiaomiev.com/");
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);

                    ds.setColor(ContextCompat.getColor(getContext(), R.color.link_blue));


                    ds.setUnderlineText(false);

                }

            }, startIndexPrivacy, endIndexPrivacy, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        privacyContentTv.setText(spannableString);

        privacyContentTv.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {

            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        }
    }


    private void openBrowser(String url) {
        try {

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            startActivity(browserIntent);

        } catch (Exception e) {

            Toast.makeText(getContext(), "无法打开浏览器，请检查网络连接或浏览器应用。", Toast.LENGTH_SHORT).show();

            e.printStackTrace();
        }
    }
}
