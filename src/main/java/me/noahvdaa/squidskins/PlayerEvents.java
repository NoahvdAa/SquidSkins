package me.noahvdaa.squidskins;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PlayerEvents implements Listener {
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		SquidSkins plugin = SquidSkins.getPlugin(SquidSkins.class);
		Player player = event.getPlayer();

		player.sendMessage(Component.text("Loading skin, please wait...", NamedTextColor.GRAY));

		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					// Get base skin.
					BufferedImage base = ImageIO.read(plugin.getResource("base.png"));

					// Download player skin.
					URL url = new URL("https://crafatar.com/skins/" + player.getUniqueId() + "?" + System.currentTimeMillis());
					BufferedImage img = ImageIO.read(url);

					// Read the face from the existing skin.
					BufferedImage face = img.getSubimage(0, 0, 64, 16);

					// Apply the face on the base.
					Graphics g = base.getGraphics();
					g.drawImage(face, 0, 0, null);
					g.dispose();

					// Prepare temp file for upload.
					File tempFile = Files.createTempFile(null, null).toFile();
					ImageIO.write(base, "png", tempFile);

					String boundary = new BigInteger(256, new Random()).toString();
					Map<Object, Object> data = new HashMap<>();
					data.put("file", tempFile.toPath());

					HttpRequest request = HttpRequest.newBuilder()
							.uri(URI.create("https://api.mineskin.org/generate/upload"))
							.header("Content-Type", "multipart/form-data;boundary=" + boundary)
							.POST(NetworkUtil.ofMimeMultipartData(data, boundary))
							.build();

					HttpClient client = HttpClient.newHttpClient();
					HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

					JSONObject jsonResponse = new JSONObject(response.body().toString());
					JSONObject texture = jsonResponse.getJSONObject("data").getJSONObject("texture");

					PlayerProfile profile = player.getPlayerProfile();
					profile.setProperty(new ProfileProperty("textures", texture.getString("value"), texture.getString("signature")));
					// We have to set the player profile sync.
					new BukkitRunnable() {
						@Override
						public void run() {
							player.sendMessage(Component.text("Your skin has been applied!", NamedTextColor.GREEN));
							player.setPlayerProfile(profile);
						}
					}.runTask(plugin);

					// Clean up temp file.
					tempFile.delete();
				} catch (IOException | InterruptedException e) {
					player.sendMessage(Component.text("Failed to update your skin. :(", NamedTextColor.RED));
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}
}
