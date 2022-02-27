package dk.sdu.subscription.Interfaces;

public interface IUser {
    String getId();
    String getName();
    String getEmail();
    String getRole();
    boolean hasAdminRights();
    String getSubtype();
    String getFamilyId();
}
