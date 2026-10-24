package lan.chaos.oauth2.auth.client;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端内存仓库（演示用；生产应落库并支持审核状态）。 */
@Repository
public class ClientRepository {

    private final Map<String, Client> store = new ConcurrentHashMap<>();

    public void save(Client client) {
        store.put(client.getClientId(), client);
    }

    public Optional<Client> findByClientId(String clientId) {
        return Optional.ofNullable(store.get(clientId));
    }
}
