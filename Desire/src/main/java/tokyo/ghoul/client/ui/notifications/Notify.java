package tokyo.ghoul.client.ui.notifications;

import lombok.Getter;
import lombok.Setter;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.math.TimerUtil;

@Getter
@Setter
public abstract class Notify implements INotify {
    private float x;
    private float y;

    private final Animation animation = new Animation(300, Easing.CUBIC_OUT);
    private final TimerUtil aliveTime = new TimerUtil();
}
