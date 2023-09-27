package io.agora.scene.base.bean;

/**
 * The type Permission item.
 */
public class PermissionItem {
    /**
     * The Permission name.
     */
    public String permissionName;
    /**
     * The Granted.
     */
    public boolean granted = false;
    /**
     * The Request id.
     */
    public int requestId;

    /**
     * Instantiates a new Permission item.
     *
     * @param name  the name
     * @param reqId the req id
     */
    public PermissionItem(String name, int reqId) {
        permissionName = name;
        requestId = reqId;
    }
}