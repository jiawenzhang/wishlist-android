package com.wish.wishlist.view;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.wish.wishlist.R;
import com.wish.wishlist.activity.WishList;
import com.wish.wishlist.util.DialogOnShowListener;

import java.io.IOException;
import android.app.Dialog;
import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.support.v7.app.AlertDialog.Builder;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;

public class ReleaseNotesView {
	private static final String TAG = WishList.LOG_TAG;
	static final private String releaseNotesXml = "release_notes"; 
	static final private String css =
				"<style type=\"text/css\">"
				+ "h1 { font-size: 12pt; }"
				+ "li { font-size: 12pt;}"
				+ "ol { padding-left: 30px;}"
				+ "</style>";
	private Activity _act;
	public ReleaseNotesView(Activity ctx) {
		_act = ctx;
	}

	private String parseReleaseNotesXML(XmlResourceParser parser) throws XmlPullParserException, IOException {
		String html = "<h1>v" + parser.getAttributeValue(null, "versionName") + "</h1><ol>";
		int eventType = parser.getEventType();
		while ((eventType != XmlPullParser.END_TAG) || (parser.getName().equals("note"))) {
			if ((eventType == XmlPullParser.START_TAG) &&(parser.getName().equals("note"))){
				eventType = parser.next();
				html = html + "<li>" + parser.getText() + "</li>";
			}
			eventType = parser.next();
		}		
		html = html + "</ol>";
		return html;
	}

	//Get the release notes from xml and return as html
	private String getReleaseNotesHtml() {
		String pkgName = _act.getPackageName();
		Resources res = _act.getResources();
		int resId = res.getIdentifier(releaseNotesXml, "xml", pkgName);
		XmlResourceParser parser = res.getXml(resId);

		String html = "<html><head>" + css + "</head><body>";
		try
		{
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if ((eventType == XmlPullParser.START_TAG) && (parser.getName().equals("release"))){
					html = html + parseReleaseNotesXML(parser);
				}
				eventType = parser.next();
			}
		} 
		catch (XmlPullParserException e)
		{
		}
		catch (IOException e)
		{
		}
		finally
		{
			parser.close();
		}
		html = html + "</body></html>";
		return html;
	}

	public void show() {
		String html = getReleaseNotesHtml();
		WebView webView = new WebView(_act);
		webView.setWebViewClient(new WebViewClient() {
		public void onPageFinished(WebView view, String url) {
			Log.d(TAG, "onPageFinished");
			Builder builder = new Builder(_act, R.style.AppCompatAlertDialogStyle)
			.setPositiveButton("Close", new Dialog.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
				}
			});
			AlertDialog dialog = builder.create();
			dialog.setTitle("Release notes");
			dialog.setView(view, 0, 0, 0, 0);
            //dialog.setOnShowListener(new DialogOnShowListener(_act));
			dialog.show();
			Log.d(TAG, "dialog show");
		}
		});
		webView.loadData(html, "text/html", "utf-8");
	}
}
