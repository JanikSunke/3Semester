package dk.sdu.subscription.ServiceLayer.DatabaseService;

import dk.sdu.subscription.Interfaces.IFamily;
import dk.sdu.subscription.Interfaces.IUser;

import java.util.List;

public class FamilyData implements IFamily {

    private String familyId;
    private List<IUser> users;

    public FamilyData(String familyId, List<IUser> users){
        this.familyId = familyId;
        this.users = users;
    }

    @Override
    public String getFamilyId() {
        return this.familyId;
    }

    @Override
    public List<IUser> getUsers() {
        return this.users;
    }
}
