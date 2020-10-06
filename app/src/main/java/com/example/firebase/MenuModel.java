package com.example.firebase;

import com.firebase.ui.auth.data.model.User;

public class MenuModel {
    private String menuText;
    private String photoUrl;
    private String user;

    public MenuModel(String menuText, String photoUrl, String user) {
        this.menuText = menuText;
        this.photoUrl = photoUrl;
        this.user = user;
    }

    public MenuModel(){};

    public String getMenuText() {
        return menuText;
    }

    public void setMenuText(String menuText) {
        this.menuText = menuText;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
