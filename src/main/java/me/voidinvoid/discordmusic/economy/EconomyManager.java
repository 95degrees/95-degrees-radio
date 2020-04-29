package me.voidinvoid.discordmusic.economy;

import com.mongodb.client.MongoCollection;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.api.entities.Member;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class EconomyManager implements RadioService {

    private MongoCollection<Document> users;
    private DatabaseManager databaseManager;

    @Override
    public void onLoad() {
        users = (databaseManager = Radio.getInstance().getService(DatabaseManager.class)).getClient().getDatabase("95degrees").getCollection("users");
    }

    public boolean makeTransaction(Member member, Transaction transaction) {

        if (member == null || member.getUser().isBot())
            return false; //TODO make a DegreeCore.jar containing all this stuff so no duplication

        if (getCoins(member) + transaction.getAmount() >= 0) {
            users.updateOne(eq(member.getUser().getId()), new Document("$inc", new Document("coins", transaction.getAmount())).append("$push", new Document("transactions", transaction)));
            return true;
        }

        return false;
        //db.getCollection("users").updateOne(eq("_id", member.getUser().getId()), new Document("$push", new Document("transaction_history", transaction)));
    }

    public int getCoins(Member member) {
        var doc = users.find(eq(member.getUser().getId())).first();
        return doc == null ? 0 : doc.getInteger("coins", 0);
    }

    public List<Document> getLeaderboard(int members) {
        return users.find().sort(new Document("coins", -1)).limit(members).into(new ArrayList<>());
    }

    public List<Transaction> getTransactions(Member who) {
        Document d = users.find(eq(who.getUser().getId())).first();
        if (d == null) return null;
        List<Document> lo = d.get("transactions", new ArrayList<>());
        return lo.stream().map(td -> {
            Transaction t = new Transaction(td.getString("_id"), TransactionType.valueOf(td.getString("type")), td.getInteger("amount"), td.getLong("timestamp"));
            if (td.containsKey("params")) {
                td.get("params", Document.class).forEach(t::addParameter);
            }
            return t;
        }).collect(Collectors.toList());
    }

    public boolean rollbackTransactions(Member who, String rollbackId) {
        List<Transaction> lt = getTransactions(who);
        Collections.reverse(lt); //order: first -> last

        int amount = 0;
        boolean found = false;

        for (Transaction t : lt) {
            amount++;
            if (t.getId().equalsIgnoreCase(rollbackId)) {
                found = true;
                break; //keep adding 1 until we find the rollback ID we need
            }
        }

        if (!found) return false;

        List<Transaction> back = lt.subList(amount, lt.size()); //these are the ones we want to keep
        Collections.reverse(back);

        int coinDiff = 0;
        for (Transaction t : lt.subList(0, amount)) { //the ones to be removed, calculate the coin difference and apply
            coinDiff -= t.getAmount();
        }

        users.updateOne(eq(who.getUser().getId()), new Document("$set", new Document("transactions", back)).append("$inc", new Document("coins", coinDiff)));

        return true;
    }
}
