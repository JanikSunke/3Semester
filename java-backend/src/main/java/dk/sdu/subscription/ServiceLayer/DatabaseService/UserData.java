package dk.sdu.subscription.ServiceLayer.DatabaseService;

import dk.sdu.subscription.Interfaces.IUser;

public class UserData implements IUser {

    private String id;
    private String name;
    private String email;
    private String role;
    private String subtype;
    private String familyId;

    public UserData(String id, String name, String email, String role, String subtype) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.subtype = subtype;
        familyId = null;
    }

    public UserData(String id, String name, String email, String role, String subtype, String familyId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.subtype = subtype;
        this.familyId = familyId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean hasAdminRights() {
        return this.role.equalsIgnoreCase("STAFF");
    }

    public String getSubtype() {
        return subtype;
    }

    @Override
    public String getFamilyId() {
        return familyId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setSubtype(String subType) {
        this.subtype = subtype;
    }

}
