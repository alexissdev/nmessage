package me.yushust.message.internal;

import me.yushust.message.MessageRepository;
import me.yushust.message.strategy.Strategy;
import me.yushust.message.StringList;
import me.yushust.message.file.NodeFile;
import me.yushust.message.allocate.NodeFilePool;
import me.yushust.message.util.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link MessageRepository} that
 * prioritizes the specified language to get messages, if
 * not found, tries with the default language file. If no
 * messages found in any file, calls {@link Strategy#getNotFoundMessage}
 */
final class MessageRepositoryImpl implements MessageRepository {

  private static final Logger LOGGER = Logger.getLogger(MessageRepository.class.getSimpleName());
  private final NodeFilePool filePool;

  private final Strategy strategy;
  private final String fileFormat;
  private final String defaultLanguage;
  private final String defaultLanguageFilename;

  MessageRepositoryImpl(MessageRepositoryBuilder builder) {
    this.filePool = NodeFilePool.createDefault(builder.nodeFileLoader, builder.loadSource);
    this.strategy = builder.strategy;
    this.fileFormat = builder.fileFormat;
    this.defaultLanguage = builder.defaultLanguage;
    this.defaultLanguageFilename = getFilename(defaultLanguage);
  }

  @Override
  public String getMessage(@Nullable String language, String path) {

    Validate.notNull(path, "path");
    NodeFile nodeFile = in(language);

    if (nodeFile != null) {
      String message = getString(nodeFile, path);
      if (message == null) {
        nodeFile = filePool.find(defaultLanguageFilename);
        if (nodeFile != null) {
          message = getString(nodeFile, path);
        }
      }
      if (message != null) {
        return message;
      }
    }

    return strategy.getNotFoundMessage(language, path);
  }

  @Override
  public StringList getMessages(@Nullable String language, String path) {

    Validate.notNull(path, "path");
    NodeFile nodeFile = in(language);

    if (nodeFile != null) {
      List<String> messages = getStringList(nodeFile, path);
      if (messages == null) {
        nodeFile = filePool.find(defaultLanguageFilename);
        if (nodeFile != null) {
          messages = getStringList(nodeFile, path);
        }
      }
      if (messages != null) {
        return new StringList(messages);
      }
    }
    return StringList.singleton(strategy.getNotFoundMessage(language, path));
  }

  private List<String> getStringList(NodeFile nodeFile, String node) {
    Object value = nodeFile.get(node);
    if (value instanceof List) {
      @SuppressWarnings("unchecked")
      List<String> messages = (List<String>) value;
      return messages;
    } else if (value != null) {
      return Arrays.asList(value.toString().split(Pattern.quote("\n")));
    } else {
      return null;
    }
  }

  private String getString(NodeFile nodeFile, String node) {
    Object value = nodeFile.get(node);
    if (value instanceof List) {
      @SuppressWarnings("unchecked")
      List<String> messages = (List<String>) value;
      return String.join("\n", messages);
    } else if (value != null) {
      return value.toString();
    } else {
      return null;
    }
  }

  @Override
  public Strategy getStrategy() {
    return strategy;
  }

  /**
   * Checks for the nodeFile with the specified {@code language},
   * if not present, tries to get the nodeFile with the default
   * language. If not found, logs a warning and returns null.
   */
  @Override
  public NodeFile in(@Nullable String language) {
    NodeFile nodeFile = null;
    if (language != null) {
      nodeFile = filePool.find(getFilename(language));
    }
    if (language == null || nodeFile == null) {
      nodeFile = filePool.find(defaultLanguageFilename);
      if (nodeFile == null) {
        LOGGER.warning("There's no a file with the default language!");
      }
    }
    return nodeFile;
  }

  @Override
  public String getDefaultLanguage() {
    return defaultLanguage;
  }

  private String getFilename(String language) {
    return fileFormat.replace("%lang%", language);
  }

}
