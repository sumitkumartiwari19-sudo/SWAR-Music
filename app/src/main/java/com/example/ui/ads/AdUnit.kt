package com.example.ui.ads

/**
 * Defines the Adsterra ad units used across SWAR Music.
 * All units are HTML/JS based and rendered via optimized WebViews.
 */
sealed class AdUnit(
    val id: String,
    val widthDp: Int,
    val heightDp: Int,
    val label: String
) {
    abstract fun getHtmlContent(): String

    /**
     * AD UNIT 1 — Banner 320×50 (persistent anchored banner)
     * Real fixed dimensions: 320×50 dp.
     */
    data class Banner320x50(
        val key: String = "8e4ab222bf8550f15132e3145ec31d0d",
        val customHtml: String? = null
    ) : AdUnit(
        id = "banner_320x50",
        widthDp = 320,
        heightDp = 50,
        label = "Banner 320×50"
    ) {
        override fun getHtmlContent(): String {
            if (!customHtml.isNullOrBlank()) return customHtml
            return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <link rel="icon" href="data:,">
                  <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                      margin: 0;
                      padding: 0;
                      width: 100%;
                      height: 100%;
                      background: transparent;
                      overflow: hidden;
                      display: flex;
                      justify-content: center;
                      align-items: center;
                    }
                    iframe {
                      border: none !important;
                      margin: 0 auto !important;
                      display: block !important;
                      width: 320px !important;
                      height: 50px !important;
                    }
                  </style>
                </head>
                <body>
                  <script type="text/javascript">
                    atOptions = {
                      'key' : '$key',
                      'format' : 'iframe',
                      'height' : 50,
                      'width' : 320,
                      'params' : {}
                    };
                  </script>
                  <script type="text/javascript" src="https://www.highrevenueformat.com/$key/invoke.js"></script>
                </body>
                </html>
            """.trimIndent()
        }
    }

    /**
     * AD UNIT 2 — 1:1 Native Banner
     * Async script + empty container div (pl31120787.profitableratecpmnetwork.com).
     * Sized to match square card dimensions (default 160×160 dp, or 150×150 dp in quick picks row).
     */
    data class NativeBanner1x1(
        val containerId: String = "container-318831072bff3d3a5d2cb9b802f24939",
        val scriptUrl: String = "https://pl31120787.profitableratecpmnetwork.com/318831072bff3d3a5d2cb9b802f24939/invoke.js",
        val customHtml: String? = null,
        val sizeDp: Int = 160
    ) : AdUnit(
        id = "native_banner_1x1",
        widthDp = sizeDp,
        heightDp = sizeDp,
        label = "Sponsored"
    ) {
        override fun getHtmlContent(): String {
            if (!customHtml.isNullOrBlank()) return customHtml
            return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <link rel="icon" href="data:,">
                  <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                      margin: 0;
                      padding: 0;
                      width: 100%;
                      height: 100%;
                      background: transparent;
                      overflow: hidden;
                      display: flex;
                      justify-content: center;
                      align-items: center;
                    }
                    #$containerId {
                      width: 100%;
                      height: 100%;
                      display: flex;
                      justify-content: center;
                      align-items: center;
                    }
                    img, iframe {
                      max-width: 100% !important;
                      max-height: 100% !important;
                      border: none !important;
                    }
                  </style>
                </head>
                <body>
                  <div id="$containerId"></div>
                  <script type="text/javascript">
                    console.log("[NativeBanner] Container mounted in DOM: " + (document.getElementById("$containerId") !== null));
                  </script>
                  <script async="async" data-cfasync="false" src="$scriptUrl"></script>
                </body>
                </html>
            """.trimIndent()
        }
    }

    /**
     * AD UNIT 3 — Banner 300×250 (Medium Rectangle)
     * High-impact placement with real fixed dimensions: 300×250 dp.
     */
    data class Banner300x250(
        val key: String = "a893f0c066d16e4c9ca99c0bd8ea2795",
        val customHtml: String? = null
    ) : AdUnit(
        id = "banner_300x250",
        widthDp = 300,
        heightDp = 250,
        label = "Featured"
    ) {
        override fun getHtmlContent(): String {
            if (!customHtml.isNullOrBlank()) return customHtml
            return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <link rel="icon" href="data:,">
                  <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                      margin: 0;
                      padding: 0;
                      width: 100%;
                      height: 100%;
                      background: transparent;
                      overflow: hidden;
                      display: flex;
                      justify-content: center;
                      align-items: center;
                    }
                    iframe {
                      border: none !important;
                      margin: 0 auto !important;
                      display: block !important;
                      width: 300px !important;
                      height: 250px !important;
                    }
                  </style>
                </head>
                <body>
                  <script type="text/javascript">
                    atOptions = {
                      'key' : '$key',
                      'format' : 'iframe',
                      'height' : 250,
                      'width' : 300,
                      'params' : {}
                    };
                  </script>
                  <script type="text/javascript" src="https://www.highrevenueformat.com/$key/invoke.js"></script>
                  <script type="text/javascript">
                    function checkFill() {
                      var ifrs = document.querySelectorAll('iframe');
                      var filled = false;
                      for (var i = 0; i < ifrs.length; i++) {
                        var r = ifrs[i].getBoundingClientRect();
                        if (r.width > 50 && r.height > 50) { filled = true; break; }
                      }
                      console.log("[AdBanner 300x250] Check fill: " + filled + ", iframes: " + ifrs.length);
                      if (window.AndroidBridge && window.AndroidBridge.onAdStatus) {
                        window.AndroidBridge.onAdStatus('$key', filled);
                      }
                    }
                    setTimeout(checkFill, 2500);
                    setTimeout(checkFill, 5000);
                  </script>
                </body>
                </html>
            """.trimIndent()
        }
    }
}
