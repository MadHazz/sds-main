package com.DevCiplak.advdisplay.Adapter;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.DevCiplak.advdisplay.Model.DataContentResponse;
import com.DevCiplak.advdisplay.Model.PageData;
import com.DevCiplak.advdisplay.Model.PageDataInfo;
import com.DevCiplak.advdisplay.Model.PageDetail;
import com.DevCiplak.advdisplay.R;
import com.DevCiplak.advdisplay.Retrofit.APIClient;
import com.DevCiplak.advdisplay.RetrofitInterfaces.DataContentInterface;
import com.DevCiplak.advdisplay.RotatedImageView;
import com.DevCiplak.advdisplay.constant.Constant;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderViewAdapter;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SliderAdapter extends SliderViewAdapter<SliderAdapter.Holder> {
    List<PageDetail> pageDetail = new ArrayList<PageDetail>();
    Context mContext;
    DataContentInterface dataContentInterface;
    String deviceId;

    public SliderAdapter(List<PageDetail> pageDetail, Context mContext, String deviceId) {
        this.pageDetail = pageDetail;
        this.mContext = mContext;
        this.deviceId = deviceId;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.slider_item, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder viewHolder, int position) {
        final PageDetail pageDetailer = pageDetail.get(position);
        String pageCode = pageDetailer.getPage_code();
        String templateID = pageDetailer.getTemplate_id();
        if (templateID.equals("3")) {
            viewHolder.dmb.setVisibility(View.VISIBLE);
            viewHolder.imageView.setVisibility(View.GONE);
            dataContentInterface = APIClient.getClient().create(DataContentInterface.class);
            dataContentInterface.getDataInfo(Constant.GET_DATA + "u=" + deviceId + "&" + "c=" + pageCode).enqueue(new Callback<DataContentResponse>() {
                @Override
                public void onResponse(Call<DataContentResponse> call, Response<DataContentResponse> response) {
                    DataContentResponse reply = response.body();
                    PageDataInfo[] pageDataInfo = reply.getPage_data();
                    for (int i = 0; i < pageDataInfo.length; i++) {
                        String slot = pageDataInfo[i].getSlot();
                        String imageUrl = pageDataInfo[i].getFilename();
                        if (slot.equals("1")) {
                            Glide.with(mContext)
                                    .load(imageUrl)
                                    .into(viewHolder.leftImg);
                        } else if (slot.equals("2")) {
                            Glide.with(mContext)
                                    .load(imageUrl)
                                    .into(viewHolder.topImg);
                        } else {
                            Glide.with(mContext)
                                    .load(imageUrl)
                                    .into(viewHolder.rightImg);
                        }
                    }
                }

                @Override
                public void onFailure(Call<DataContentResponse> call, Throwable t) {
                    Toast.makeText(mContext, "No Internet Connection! Please make sure your device connected to the internet", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            viewHolder.dmb.setVisibility(View.GONE);
            viewHolder.imageView.setVisibility(View.VISIBLE);
            dataContentInterface = APIClient.getClient().create(DataContentInterface.class);
            dataContentInterface.getDataInfo(Constant.GET_DATA + "u=" + deviceId + "&" + "c=" + pageCode).enqueue(new Callback<DataContentResponse>() {
                @Override
                public void onResponse(Call<DataContentResponse> call, Response<DataContentResponse> response) {
                    DataContentResponse reply = response.body();
                    if (response.isSuccessful()) {
                        if (reply != null) {
                            if (reply.getStatus().equals(true)) {
                                PageDataInfo[] pageDataInfo = reply.getPage_data();
                                for (int i = 0; i < pageDataInfo.length; i++) {
                                    String imageUrl = pageDataInfo[i].getFilename();
                                    Glide.with(mContext)
                                            .load(imageUrl)
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(viewHolder.imageView);
                                }
                            } else {
                                Toast.makeText(mContext, reply.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<DataContentResponse> call, Throwable t) {
                    Toast.makeText(mContext, "No Internet Connection! Please make sure your device connected to the internet", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public int getCount() {
        return pageDetail.size();
    }

    public class Holder extends SliderViewAdapter.ViewHolder {
        RotatedImageView imageView;
        ImageView topImg, leftImg, rightImg;
        ConstraintLayout dmb;

        public Holder(View itemView) {
            super(itemView);
            dmb = itemView.findViewById(R.id.dmb);
            topImg = itemView.findViewById(R.id.topImg);
            leftImg = itemView.findViewById(R.id.leftImg);
            rightImg = itemView.findViewById(R.id.rightImg);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}