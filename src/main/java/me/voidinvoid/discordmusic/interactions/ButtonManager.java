package me.voidinvoid.discordmusic.interactions;

import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.ActionRow;
import net.dv8tion.jda.api.interactions.button.Button;
import net.dv8tion.jda.api.interactions.button.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.interactions.ButtonImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ButtonManager extends ListenerAdapter implements RadioService {

    private Map<String, ButtonData> buttonData = new HashMap<>();

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent e) {
        if (e.getButton() == null) {
            log("Received null button click");
            return;
        }

        var data = buttonData.get(e.getButton().getId());

        if (data != null && e.getButton() != null) {
            log("Invoking button handler for button " + e.getButton().getId());

            data.getHandler().accept(new ButtonEvent(e, data));
            if (data.isDirty()) {
                buttonData.remove(e.getButton().getId());
                buttonData.put(data.getFullId(), data);
                e.updateButton(new ButtonImpl(data.getFullId(), e.getButton().getLabel(), e.getButton().getStyle(), e.getButton().getUrl(), e.getButton().isDisabled(), e.getButton().getEmoji())).queue();
            }
        }
    }

    public void addCallback(Button button, Consumer<ButtonEvent> handler) {
        buttonData.put(button.getId(), new ButtonData(button.getId(), handler));
    }

    public void removeCallback(Button button) {
        buttonData.remove(button.getId());
    }

    public static Button of(ButtonStyle style, String label, Consumer<ButtonEvent> handler) {
        var id = UUID.randomUUID().toString();
        Button button = Button.of(style, id, label);

        Service.of(ButtonManager.class).addCallback(button, handler);

        return button;
    }

    public static Button of(ButtonStyle style, Emoji emoji, Consumer<ButtonEvent> handler) {
        var id = UUID.randomUUID().toString();
        Button button = Button.of(style, id, emoji);

        Service.of(ButtonManager.class).addCallback(button, handler);

        return button;
    }

    public static Button of(ButtonStyle style, String label, Emoji emoji, Consumer<ButtonEvent> handler) {
        var id = UUID.randomUUID().toString();
        Button button = new ButtonImpl(id, label, style, false, emoji);

        Service.of(ButtonManager.class).addCallback(button, handler);

        return button;
    }

    public static MessageAction applyButtons(MessageAction action, List<Button> buttons) {
        return applyButtons(action, buttons.toArray(new Button[] {}));
    }

    public static MessageAction applyButtons(MessageAction action, Button... buttons) {
        if (buttons.length == 0) {
            return action;
        }

        var actionRows = new ActionRow[(int) Math.ceil((double) buttons.length / 5)];
        var activeActionRow = 0;

        int ix = 0;
        for (var button : buttons) { //since each action row has 5 button limit, create and allocate action rows/buttons accordingly
            ix++;

            var row = actionRows[activeActionRow];

            if (row == null) {
                actionRows[activeActionRow] = ActionRow.of(button);
            } else {
                row.getComponents().add(button);
            }

            if (ix == 5) {
                activeActionRow++; //5 in this component, move to next...
                ix = 0;
            }
        }

        return action.setActionRows(actionRows);
    }
}
