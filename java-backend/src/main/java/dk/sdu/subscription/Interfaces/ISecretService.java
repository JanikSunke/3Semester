package dk.sdu.subscription.Interfaces;

public interface ISecretService {
    public String getStringSecret(String key);
    public Integer getIntSecret(String key);
    public Boolean getBooleanSecret(String key);
}
