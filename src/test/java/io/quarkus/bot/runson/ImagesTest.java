package io.quarkus.bot.runson;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ImagesTest {

    @Test
    public void testImage() {
        assertThat(Images.getUbuntuLatest("main")).isEqualTo(Images.UBUNTU_24);
        assertThat(Images.getUbuntuLatest("foobar")).isEqualTo(Images.UBUNTU_24);
        assertThat(Images.getUbuntuLatest("3.31")).isEqualTo(Images.UBUNTU_24);
        assertThat(Images.getUbuntuLatest("3.27")).isEqualTo(Images.UBUNTU_22);
        assertThat(Images.getUbuntuLatest("3.20")).isEqualTo(Images.UBUNTU_22);
    }
}
