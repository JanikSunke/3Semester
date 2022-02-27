package dk.sdu.subscription.Interfaces;

public interface IUserService {

    IUser getUserFromId(String id, boolean useOwnDB) throws Exception;
}
