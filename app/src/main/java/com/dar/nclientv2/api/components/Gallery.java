package com.dar.nclientv2.api.components;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import com.dar.nclientv2.api.enums.ImageType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class Gallery extends GenericGallery{




    private Date uploadDate;
    private int favoriteCount,id,pageCount,mediaId;
    private String titles[]=new String[3],scanlator;
    private Tag[][] tags;
    private Image cover,thumbnail;
    private Page pages[];
    private Language language= Language.UNKNOWN;
    private boolean valid;

    @Override
    public boolean isValid() {
        return valid;
    }

    public Gallery(JsonReader jr) throws IOException {
        valid=true;
        jr.beginObject();
        while(jr.peek()!= JsonToken.END_OBJECT){
            switch(jr.nextName()){
                case "upload_date":uploadDate=new Date(jr.nextLong()*1000);break;
                case "num_favorites":favoriteCount=jr.nextInt();break;
                case "media_id":mediaId=jr.nextInt();break;
                case "title":readTitles(jr);break;
                case "images":readImages(jr); break;
                case "scanlator":scanlator=jr.nextString();break;
                case "tags":readTags(jr);break;
                case "id":id=jr.nextInt();break;
                case "num_pages":pageCount=jr.nextInt();break;
                case "error":jr.skipValue(); valid=false;

            }
        }
        jr.endObject();
    }

    public static final Creator<Gallery> CREATOR = new Creator<Gallery>() {
        @Override
        public Gallery createFromParcel(Parcel in) {
            try {
                Log.d(Global.LOGTAG,"Reading to parcel");
                return new Gallery(in);
            } catch (IOException e) {
                Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
            }
            return null;
        }

        @Override
        public Gallery[] newArray(int size) {
            return new Gallery[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
    private Gallery(Parcel in) throws IOException {
        this(new JsonReader(new StringReader(in.readString())));
        Log.d( Global.LOGTAG,toString());
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        try {
            Log.d(Global.LOGTAG,"Writing to parcel");
            dest.writeString(toJSON());
        } catch (IOException e) {
            Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
        }
    }

    private void readTags(JsonReader jr)throws IOException {
        List<Tag>t=new ArrayList<>();
        jr.beginArray();
        while(jr.hasNext())t.add(new Tag(jr));
        jr.endArray();
        Collections.sort(t, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                int x=o1.getType().ordinal()-o2.getType().ordinal();
                if(x==0)return o1.getCount()-o2.getCount();
                return x;
            }
        });
        tags=new Tag[TagType.values().length][];
        int i=0;
        TagType type=t.get(0).getType();
        for(int a=1;a<t.size();a++){
            TagType tag=t.get(a).getType();
            if(tag!=type){
                tags[type.ordinal()]=new Tag[a-i];
                tags[type.ordinal()]=t.subList(i,a).toArray(tags[type.ordinal()]);
                i=a;
                type=tag;
            }
        }
        tags[type.ordinal()]=new Tag[t.size()-i];
        tags[type.ordinal()]=t.subList(i,t.size()).toArray(tags[type.ordinal()]);
        //CHINESE 29963 ENGLISH 12227 JAPANESE 6346
        for(Tag tag:tags[TagType.LANGUAGE.ordinal()]){
            switch (tag.getId()){
                case 6346 :language= Language.JAPANESE;break;
                case 12227:language= Language.ENGLISH;break;
                case 29963:language= Language.CHINESE;break;
            }
            if(language!=Language.UNKNOWN)break;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append("Gallery{" + "uploadDate=").append(uploadDate).
                append(", favoriteCount=").append(favoriteCount).
                append(", id=").append(id).
                append(", pageCount=").append(pageCount)
                .append(", mediaId=").append(mediaId)
                .append(", titles=").append(Arrays.toString(titles))
                .append(", scanlator='").append(scanlator).append('\'')
                .append(", tags={");
        int len=tags.length;
                for(int a=0;a<len;a++){
                    if(tags[a]!=null)
                        builder.append(TagType.values()[a]).append(Arrays.toString(tags[a])).append(',');
                }
                builder.append("}, cover=").append(cover).append(", thumbnail=").append(thumbnail).append(", pages=").append(Arrays.toString(pages)).append('}');
                return builder.toString();
    }

    private void readImages(JsonReader jr) throws IOException {

        List<Page>p=new ArrayList<>();
        jr.beginObject();
        int i=1;
        while (jr.peek()!=JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "cover":cover=new Image(jr,mediaId, ImageType.COVER);break;
                case "pages":jr.beginArray();while(jr.hasNext())p.add(new Page(jr,mediaId,i++));jr.endArray();break;
                case "thumbnail":thumbnail=new Image(jr,mediaId,ImageType.THUMBNAIL);break;
            }
        }
        jr.endObject();
        pages=new Page[p.size()];
        pages=p.toArray(pages);
    }
    private String unescapeUnicodeString(String t){
        StringBuilder s=new StringBuilder();
        int l=t.length();
        for(int a=0;a<l;a++){
            if(t.charAt(a)=='\\'&&t.charAt(a+1)=='u'){
                System.out.println(t.substring(a));
                s.append((char) Integer.parseInt( t.substring(a+2,a+6), 16 ));
                a+=5;
            }else s.append(t.charAt(a));
        }
        return s.toString();
    }
    private void setTitle(TitleType x, String t){
        titles[x.ordinal()]= unescapeUnicodeString(t);
    }
    private void readTitles(JsonReader jr) throws IOException {
        jr.beginObject();
        while(jr.peek()!=JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "japanese":if(jr.peek()!= JsonToken.NULL) setTitle(TitleType.JAPANESE,jr.nextString());else jr.skipValue();break;
                case "english": if(jr.peek()!= JsonToken.NULL) setTitle(TitleType.ENGLISH ,jr.nextString());else jr.skipValue();break;
                case "pretty":  if(jr.peek()!= JsonToken.NULL) setTitle(TitleType.PRETTY  ,jr.nextString());else jr.skipValue();break;
            }
        }
        jr.endObject();
    }

    @Override
    public String getTitle() {
        return getTitle(Global.getTitleType());
    }

    public String getTitle(int x){
        return titles[x];
    }
    public String getTitle(TitleType x){
        return getTitle(x.ordinal());
    }

    public Language getLanguage() {
        return language;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }
    @Override
    public int getId() {
        return id;
    }
    @Override
    public int getPageCount() {
        return pageCount;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    public int getMediaId() {
        return mediaId;
    }

    public String getScanlator() {
        return scanlator;
    }

    public String[] getTitles() {
        return titles;
    }

    private Image getCover() {
        return cover;
    }

    public Image getThumbnail() {
        return thumbnail;
    }
    public int getTagCount(@NonNull TagType type){
        return getTagCount(type.ordinal());
    }

    private int getTagCount(int type) {
        if(type<0||type>tags.length||tags[type]==null)return 0;
        return tags[type].length;
    }
    private Tag getTag(int type,int index){
        if(type<0||index<0||type>tags.length||tags[type]==null||index>tags[type].length)return null;
        return tags[type][index];
    }
    public Tag getTag(@NonNull TagType type,int index){
        return getTag(type.ordinal(),index);
    }
    public Page getPage(int index){
        return pages[index];
    }
    private String toJSON() throws IOException{
        StringWriter sw=new StringWriter();
        JsonWriter jw=new JsonWriter(sw);
        jw.beginObject();
        jw.name("id").value(id);
        jw.name("media_id").value(mediaId);
        jw.name("title").beginObject()
                .name("english").value(getTitle(TitleType.ENGLISH))
                .name("japanese").value(getTitle(TitleType.JAPANESE))
                .name("pretty").value(getTitle(TitleType.PRETTY)).endObject();
        jw.name("images").beginObject().name("pages").beginArray();
        pageLoader(ImageType.PAGE,jw);
        jw.endArray();
        jw.name("cover");
        pageLoader(ImageType.COVER,jw);
        jw.name("thumbnail");
        pageLoader(ImageType.THUMBNAIL,jw);
        jw.endObject();
        jw.name("scanlator").value(scanlator);
        jw.name("upload_date").value(uploadDate.getTime()/1000);
        jw.name("tags").beginArray();
        tagsLoader(jw);
        jw.endArray();
        jw.name("num_pages").value(pageCount);
        jw.name("num_favorites").value(favoriteCount);
        jw.endObject();
        return  sw.toString();
    }
    private void tagsLoader(JsonWriter jw) throws IOException{
        for(Tag[]type:tags)
            if(type!=null)
            for(Tag x:type){
                jw.beginObject();
                jw.name("id").value(x.getId());
                jw.name("type").value(x.findTagString());
                jw.name("name").value(x.getName());
                jw.name("count").value(x.getCount());
                jw.endObject();
            }
    }
    private void pageLoader(ImageType type,JsonWriter jw) throws IOException{
        switch (type){
            case PAGE:
                for(Page x:pages)
                    jw.beginObject().name("t").value(x.jpg?"j":"p").endObject();
                break;
            default:
                jw.beginObject().name("t").value((type==ImageType.COVER?cover:thumbnail).jpg?"j":"p").endObject();
        }
    }
    public String writeGallery() throws IOException{
        StringWriter stringWriter=new StringWriter();
        JsonWriter jw=new JsonWriter(stringWriter);
        jw.beginArray();
        jw.value(id).value(mediaId).value(language.ordinal()).value(favoriteCount).value(pageCount).value(uploadDate.getTime()).value(scanlator);
        jw.value(titles[0]).value(titles[1]).value(titles[2]);
        jw.value(getThumbnail().jpg?"j":"p").value(getCover().jpg?"j":"p");
        boolean allPng=true,allJpg=true;
        StringBuilder builder=new StringBuilder(pageCount);
        for(Page p:pages){
            if(p.jpg)allPng=false;
            else allJpg=false;
            builder.append(p.jpg?"j":"p");
        }
        if(!allPng&&!allJpg){
            jw.value(builder.toString());
        }else jw.value(allJpg?"j":"p");
        for(Tag[] array:tags){
            jw.beginArray();
            if(array!=null)
                for(Tag x:array) {
                    jw.value(x.getName()).value(x.getCount()).value(x.getId());
                }
            jw.endArray();
        }
        String fin=stringWriter.toString();
        stringWriter.close();
        return fin;
    }
    public Gallery(String x) throws IOException{
        //Vero inizio
        JsonReader jr=new JsonReader(new StringReader("{\"f\":"+x+"]}"));
        jr.beginObject();
        jr.skipValue();
        jr.beginArray();
        id=jr.nextInt();
        mediaId=jr.nextInt();
        language=Language.values()[jr.nextInt()];
        favoriteCount=jr.nextInt();
        pageCount=jr.nextInt();
        uploadDate=new Date(jr.nextLong());
        scanlator=jr.nextString();
        titles[0]=jr.nextString();
        titles[1]=jr.nextString();
        titles[2]=jr.nextString();
        thumbnail=new Image(jr.nextString().equals("j"),mediaId,ImageType.THUMBNAIL);
        cover=new Image(jr.nextString().equals("j"),mediaId,ImageType.COVER);
        pages=new Page[pageCount];
        String pagstr=jr.nextString();
        if(pagstr.length()==1){
            for(int a=0;a<pageCount;a++)pages[a]=new Page(pagstr.equals("j"),mediaId,a+1);
        }else for(int a=0;a<pageCount;a++)pages[a]=new Page(pagstr.charAt(a)=='j',mediaId,a+1);
        int len=TagType.values().length;
        tags=new Tag[len][];
        for(int a=0;a<len;a++){
            List<Tag>list=new ArrayList<>();
            jr.beginArray();
            while (jr.hasNext()) {
                list.add(new Tag(jr.nextString(),jr.nextInt(),jr.nextInt(),TagType.values()[a]));
            }
            if(list.size()>0) tags[a]=list.toArray(new Tag[0]);
            jr.endArray();
        }
        jr.close();
    }
}
