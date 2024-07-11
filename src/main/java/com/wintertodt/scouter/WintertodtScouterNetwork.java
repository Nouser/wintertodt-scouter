/*
 * Copyright (c) 2019, Weird Gloop <admin@weirdgloop.org>
 * Copyright (c) 2021, Andrew McAdams
 * Copyright (c) 2021, nucleon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.wintertodt.scouter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static okhttp3.CacheControl.FORCE_NETWORK;

@Slf4j
@Singleton
public class WintertodtScouterNetwork
{
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private Gson gson;

    @Inject
    private WintertodtScouterPlugin plugin;


    protected void submitToAPI(WintertodtBossData data)
    {
        List<Object> temp = new ArrayList<>();
        synchronized (this)
        {
            if (data == null)
            {
                return;
            }
            temp.add(data);
        }
        makePostRequest(temp);
    }

    private ArrayList<WintertodtBossData> parseData(JsonArray j)
    {

        ArrayList<WintertodtBossData> list = new ArrayList<WintertodtBossData>();
        for (JsonElement jsonElement : j)
        {
            JsonObject jObj = jsonElement.getAsJsonObject();
            try
            {
                WintertodtBossData globalBossData = new WintertodtBossData(jObj.get("a").getAsInt(), jObj.get("b").getAsInt(), jObj.get("c").getAsLong(), false, jObj.get("d").getAsInt());
                list.add(globalBossData);
            } catch (UnsupportedOperationException uos) {
                log.error("Boss Data Json Error: " + uos.getLocalizedMessage());
            }
        }
        return list;
    }

    protected void makePostRequest(List<Object> temp)
    {
        try
        {
            Request r = new Request.Builder()
                    .url(plugin.getWintertodtGetUplink())
                    .addHeader("Authorization", "post")
                    .post(RequestBody.create(JSON, gson.toJson(temp)))
                    .build();

            okHttpClient.newCall(r).enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.debug("Error sending wintertodt boss data", e);
                    plugin.setPostError(true);
                }

                @Override
                public void onResponse(Call call, Response response)
                {
                    if (response.isSuccessful())
                    {

                        log.debug("Successfully sent wintertodt boss data");
                        plugin.setPostError(false);
                        response.close();
                    }
                    else
                    {
                        log.debug("Post request unsuccessful");
                        plugin.setPostError(true);
                    }
                }
            });
        }
        catch (IllegalArgumentException e)
        {
            log.error("Bad URL given: " + e.getLocalizedMessage());
            plugin.setPostError(true);
        }
    }

    protected void makeGetRequest()
    {
        try
        {
            Request r = new Request.Builder()
                    .url(plugin.getWintertodtGetDownlink())
                    .addHeader("Authorization", plugin.apiVersion)
                    .cacheControl(FORCE_NETWORK)
                    .build();
            okHttpClient.newCall(r).enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.debug("Error retrieving wintertodt boss data", e);
                    plugin.setGetError(true);
                }

                @Override
                public void onResponse(Call call, Response response)
                {
                    if (response.isSuccessful())
                    {
                        try
                        {
                            JsonArray j = gson.fromJson(response.body().string(), JsonArray.class);
                            try {
                                plugin.setGlobalBossDataArrayList(parseData(j));
                            } catch (NullPointerException e) {
                                log.error("null data from downlink: "+e.getMessage());
                            }
                            log.debug(j.toString());
                            plugin.setGetError(false);
                            plugin.updatePanelList();
                            response.close();
                        }
                        catch (IOException | JsonSyntaxException e)
                        {
                            plugin.setGetError(true);
                            log.error(e.getMessage());
                            response.close();
                        }
                    }
                    else
                    {
                        log.error("Get request unsuccessful");
                        plugin.setGetError(true);
                        response.close();;
                    }
                }
            });
        }
        catch (IllegalArgumentException e)
        {
            log.error("Bad URL given: " + e.getLocalizedMessage());
        }
    }
}
