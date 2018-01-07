/*
 * Copyright 2016 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.odk.collect.android.R;

public class OpenSourceLicensesActivity extends Activity {
    private static final String LICENSES_HTML_PATH = "file:///android_asset/open_source_licenses.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_source_licenses);

        WebView webViewOpenSourceLicenses = (WebView) findViewById(R.id.web_view_open_source_licenses);
        webViewOpenSourceLicenses.getSettings().setLoadWithOverviewMode(true);
        webViewOpenSourceLicenses.getSettings().setUseWideViewPort(true);
        webViewOpenSourceLicenses.getSettings().setTextSize(WebSettings.TextSize.LARGEST);
        webViewOpenSourceLicenses.loadUrl(LICENSES_HTML_PATH);
    }
}
