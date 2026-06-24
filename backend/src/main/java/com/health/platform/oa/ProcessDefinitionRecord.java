package com.health.platform.oa;

import java.util.ArrayList;
import java.util.List;

public class ProcessDefinitionRecord {
    private final long id;
    private final String code;
    private final String name;
    private final int version;
    private boolean enabled = true;
    private final List<ProcessNodeRecord> nodes = new ArrayList<>();

    public ProcessDefinitionRecord(long id, String code, String name, int version) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.version = version;
    }

    public long id() { return id; }
    public String code() { return code; }
    public String name() { return name; }
    public int version() { return version; }
    public boolean enabled() { return enabled; }
    public List<ProcessNodeRecord> nodes() { return nodes; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getId() { return id(); }
    public String getCode() { return code(); }
    public String getName() { return name(); }
    public int getVersion() { return version(); }
    public boolean isEnabled() { return enabled(); }
    public List<ProcessNodeRecord> getNodes() { return nodes(); }

}
