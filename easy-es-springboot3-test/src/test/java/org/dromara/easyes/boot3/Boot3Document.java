package org.dromara.easyes.boot3;

import org.dromara.easyes.annotation.IndexId;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.rely.IdType;
import org.dromara.easyes.annotation.rely.RefreshPolicy;

@IndexName(value = Boot3Document.INDEX_NAME, refreshPolicy = RefreshPolicy.IMMEDIATE)
public class Boot3Document {

    static final String INDEX_NAME = "easy_es_boot3_es8_compatibility";

    @IndexId(type = IdType.CUSTOMIZE, writeToSource = true)
    private String id;

    private String title;

    public String getId() {
        return id;
    }

    public Boot3Document setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Boot3Document setTitle(String title) {
        this.title = title;
        return this;
    }
}
