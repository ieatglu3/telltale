package com.github.ieatglu3.telltale;

import java.util.*;

/**
 * Minimalist command framework
 */
public final class Telltale
{

  /**
   * Base class for exceptions thrown by the command framework
   */
  public static abstract class CommandException extends Throwable
  {
    public abstract void accept(CommandExceptionVisitor visitor);
  }

  public static final class Exceptions
  {

    /** Base for exceptions caused by what the caller typed, rather than by how the commands were defined */
    public abstract static class CommandInputException extends CommandException
    {
      CommandInputException() {}
    }

    public static class Foreign extends CommandException
    {
      private final Throwable cause;
      public Foreign(Throwable cause)
      {
        this.cause = cause;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public Throwable cause()
      {
        return this.cause;
      }
    }

    /** Thrown when a registry is called with no arguments at all */
    public static final class NoCommandProvidedException extends CommandInputException
    {
      public NoCommandProvidedException() {}

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }
    }

    /** Thrown when a registry is called with an alias it does not contain */
    public static final class UnknownCommandException extends CommandInputException
    {
      private final String alias;

      public UnknownCommandException(String alias)
      {
        this.alias = alias;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public String alias()
      {
        return this.alias;
      }
    }

    /** Thrown when too many arguments are provided to a command */
    public static final class TooManyArgumentsException extends CommandInputException
    {
      private final int provided;
      private final int expected;

      public TooManyArgumentsException(int provided, int expected)
      {
        this.provided = provided;
        this.expected = expected;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public int provided()
      {
        return this.provided;
      }

      public int expected()
      {
        return this.expected;
      }
    }

    /** Thrown when a required argument is missing from a command invocation */
    public static final class MissingRequiredArgumentException extends CommandInputException
    {
      private final String argumentName;
      private final int index;

      public MissingRequiredArgumentException(String argumentName, int index)
      {
        this.argumentName = argumentName;
        this.index = index;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public String argumentName()
      {
        return this.argumentName;
      }

      public int index()
      {
        return this.index;
      }
    }

    /** Thrown when two commands in a registry share an alias */
    public static final class DuplicateCommandAliasException extends CommandException
    {
      private final String alias;

      public DuplicateCommandAliasException(String alias)
      {
        this.alias = alias;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public String alias()
      {
        return this.alias;
      }
    }

    /** Thrown when two sub-commands of the same command share an alias */
    public static final class DuplicateSubCommandAliasException extends CommandException
    {
      private final String alias;

      public DuplicateSubCommandAliasException(String alias)
      {
        this.alias = alias;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public String alias()
      {
        return this.alias;
      }
    }

    /** Thrown when two argument templates of the same command share a name */
    public static final class DuplicateArgumentTemplateException extends CommandException
    {
      private final String name;

      public DuplicateArgumentTemplateException(String name)
      {
        this.name = name;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public String name()
      {
        return this.name;
      }
    }

    /** Thrown when a required argument template is declared after an optional one */
    public static final class RequiredAfterOptionalArgumentException extends CommandException
    {
      private final String name;
      private final int index;

      public RequiredAfterOptionalArgumentException(String name, int index)
      {
        this.name = name;
        this.index = index;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public String name()
      {
        return this.name;
      }

      public int index()
      {
        return this.index;
      }
    }

    /** Thrown when an argument is looked up by a name that was not provided */
    public static final class NoSuchArgumentException extends CommandException
    {
      private final String name;

      public NoSuchArgumentException(String name)
      {
        this.name = name;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public String name()
      {
        return this.name;
      }
    }

    /** Thrown when an argument is looked up by an index outside the provided arguments */
    public static final class ArgumentIndexOutOfBoundsException extends CommandException
    {
      private final int index;
      private final int size;

      public ArgumentIndexOutOfBoundsException(int index, int size)
      {
        this.index = index;
        this.size = size;
      }

      @Override public void accept(CommandExceptionVisitor visitor) { visitor.visit(this); }

      public int index()
      {
        return this.index;
      }

      public int size()
      {
        return this.size;
      }
    }
  }

  /**
   * Visitor for exceptions thrown by the command framework
   */
  public interface CommandExceptionVisitor
  {
    void visit(Exceptions.Foreign e);
    void visit(Exceptions.NoCommandProvidedException e);
    void visit(Exceptions.UnknownCommandException e);
    void visit(Exceptions.TooManyArgumentsException e);
    void visit(Exceptions.MissingRequiredArgumentException e);
    void visit(Exceptions.DuplicateCommandAliasException e);
    void visit(Exceptions.DuplicateSubCommandAliasException e);
    void visit(Exceptions.DuplicateArgumentTemplateException e);
    void visit(Exceptions.RequiredAfterOptionalArgumentException e);
    void visit(Exceptions.NoSuchArgumentException e);
    void visit(Exceptions.ArgumentIndexOutOfBoundsException e);
  }

  /**
   * Represents a command
   *
   * @param <Caller> the type of the caller of the command
   * @param <Ctx>    the type of the context object that will be passed to the command handlers
   * @param <Permission> the type of the permission object
   */
  public static final class Command<Caller extends Telltale.Caller<Permission>, Ctx, Permission>
  {

    public static final class Builder<Caller extends Telltale.Caller<Permission>, Ctx, Permission>
    {
      private String description = "";
      private final LinkedHashSet<String> aliases = new LinkedHashSet<>();
      private final HashSet<Permission> permissions = new HashSet<>();
      private CommandInvocationHandler<Caller, Ctx, Permission> invoke = null;
      private CommandSuggestionHandler<Caller, Ctx, Permission> suggestionHandler = null;
      private CommandExceptionHandler<Caller, Ctx, Permission> exceptionHandler = null;
      private final ArrayList<Command<Caller, Ctx, Permission>> subCommands = new ArrayList<>();
      private final ArrayList<ArgumentTemplate> argumentTemplates = new ArrayList<>();

      /**
       * Sets the description of the command
       *
       * @param description the description of the command
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withDescription(String description)
      {
        this.description = description;
        return this;
      }

      /**
       * Adds an argument template to the command
       *
       * @param template the argument template to add
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withArgumentTemplate(ArgumentTemplate template)
      {
        this.argumentTemplates.add(template);
        return this;
      }

      /**
       * Adds an alias to the command
       *
       * @param alias the alias to add
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withAlias(String alias)
      {
        this.aliases.add(alias.toLowerCase());
        return this;
      }

      /**
       * Adds multiple aliases to the command
       *
       * @param aliases the aliases to add
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withAliases(Collection<String> aliases)
      {
        for (final String alias : aliases)
          this.aliases.add(alias.toLowerCase());
        return this;
      }

      /**
       * Adds a permission to the command
       *
       * @param permission the permission to add
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withPermission(Permission permission)
      {
        this.permissions.add(permission);
        return this;
      }

      /**
       * Adds multiple permissions to the command
       *
       * @param permissions the permissions to add
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withPermissions(Collection<Permission> permissions)
      {
        this.permissions.addAll(permissions);
        return this;
      }

      /**
       * Sets the invoke handler for this command
       *
       * @param invoke the invoke handler
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withInvoke(CommandInvocationHandler<Caller, Ctx, Permission> invoke)
      {
        this.invoke = invoke;
        return this;
      }

      /**
       * Sets the suggestion handler for this command
       *
       * @param suggestionHandler the suggestion handler
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withSuggestions(CommandSuggestionHandler<Caller, Ctx, Permission> suggestionHandler)
      {
        this.suggestionHandler = suggestionHandler;
        return this;
      }

      /**
       * Sets the exception handler for this command
       *
       * @param exceptionHandler the exception handler
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withExceptionHandler(CommandExceptionHandler<Caller, Ctx, Permission> exceptionHandler)
      {
        this.exceptionHandler = exceptionHandler;
        return this;
      }

      /**
       * Adds a sub-command to this command
       *
       * @param subCommand the sub-command to add
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withSubCommand(Command<Caller, Ctx, Permission> subCommand)
      {
        this.subCommands.add(subCommand);
        return this;
      }

      /**
       * Builds the command
       *
       * @return the built command
       * @throws Exceptions.DuplicateSubCommandAliasException if two sub-commands share an alias
       * @throws Exceptions.DuplicateArgumentTemplateException if two argument templates share a name
       * @throws Exceptions.RequiredAfterOptionalArgumentException if a required argument template is declared after an optional one
       */
      @SuppressWarnings("unchecked")
      public Command<Caller, Ctx, Permission> build() throws
        Exceptions.DuplicateSubCommandAliasException,
        Exceptions.DuplicateArgumentTemplateException,
        Exceptions.RequiredAfterOptionalArgumentException
      {
        return new Command<>(
          this.description,
          new ArrayList<>(this.aliases),
          new ArrayList<>(this.permissions),
          this.invoke,
          this.suggestionHandler,
          this.exceptionHandler,
          this.subCommands.toArray(new Command[0]),
          new ArgumentTemplateMap(this.argumentTemplates.toArray(new ArgumentTemplate[0]))
        );
      }
    }

    private final String description;
    private final LinkedHashSet<String> aliases;
    private final HashSet<Permission> permissions;
    private final CommandInvocationHandler<Caller, Ctx, Permission> invoke;
    private final CommandSuggestionHandler<Caller, Ctx, Permission> suggestionHandler;
    private final CommandExceptionHandler<Caller, Ctx, Permission> exceptionHandler;
    private final List<Command<Caller, Ctx, Permission>> subCommands;
    private final HashMap<String, Command<Caller, Ctx, Permission>> subCommandsByAlias;

    private final ArgumentTemplateMap argumentTemplateMap;

    Command(
      String description,
      Collection<String> aliases,
      Collection<Permission> permissions,
      CommandInvocationHandler<Caller, Ctx, Permission> invoke,
      CommandSuggestionHandler<Caller, Ctx, Permission> suggestionHandler,
      CommandExceptionHandler<Caller, Ctx, Permission> exceptionHandler,
      Command<Caller, Ctx, Permission>[] subCommands,
      ArgumentTemplateMap argumentTemplateMap
    ) throws Exceptions.DuplicateSubCommandAliasException
    {
      this.description = description;
      this.aliases = new LinkedHashSet<>(aliases);
      this.permissions = new HashSet<>(permissions);
      this.invoke = invoke;
      this.suggestionHandler = suggestionHandler;
      this.exceptionHandler = exceptionHandler;
      this.subCommands = List.of(subCommands);

      final HashMap<String, Command<Caller, Ctx, Permission>> subCommandsByAlias = new HashMap<>(subCommands.length);
      for (final Command<Caller, Ctx, Permission> subCommand : subCommands)
      {
        for (final String alias : subCommand.aliases())
        {
          if (subCommandsByAlias.containsKey(alias))
            throw new Exceptions.DuplicateSubCommandAliasException(alias);
          subCommandsByAlias.put(alias, subCommand);
        }
      }

      this.subCommandsByAlias = subCommandsByAlias;
      this.argumentTemplateMap = argumentTemplateMap;
    }

    /** @return the description of the command */
    public String description()
    {
      return this.description;
    }

    /** @return the first alias the command was declared with */
    public String primaryAlias()
    {
      return this.aliases.iterator().next();
    }

    /** @return true if the command has an invoke handler of its own */
    public boolean isInvokable()
    {
      return this.invoke != null;
    }

    /** @return true if the caller has every permission this command requires */
    public boolean isPermitted(Caller caller)
    {
      for (final Permission permission : this.permissions)
      {
        if (!caller.hasPermission(permission))
          return false;
      }
      return true;
    }

    /** @return an unmodifiable list of the sub-commands, in the order they were added */
    public List<Command<Caller, Ctx, Permission>> subCommands()
    {
      return this.subCommands;
    }

    /** @return an unmodifiable list of the argument templates, in the order they were added */
    public List<ArgumentTemplate> argumentTemplates()
    {
      return List.of(this.argumentTemplateMap.templates);
    }

    /** @return the sub-command with the specified alias, or null if no such sub-command exists */
    public Optional<Command<Caller, Ctx, Permission>> subCommand(String alias)
    {
      return Optional.ofNullable(this.subCommandOrNull(alias));
    }

    /** @return the sub-command with the specified alias, or null if no such sub-command exists */
    public Command<Caller, Ctx, Permission> subCommandOrNull(String alias)
    {
      return this.subCommandsByAlias.get(alias.toLowerCase());
    }

    /** @return an unmodifiable set of the aliases of this command */
    public Set<String> aliases()
    {
      return Collections.unmodifiableSet(this.aliases);
    }

    /** @return an unmodifiable set of the permissions required to execute this command */
    public Set<Permission> permissions()
    {
      return Collections.unmodifiableSet(this.permissions);
    }

    /**
     * Invokes the command with the provided caller, context, and arguments
     * @param caller  the caller of the command
     * @param context the context object that will be passed to the command handlers
     * @param args    the arguments of the command
     * @return invocation result containing the missing permissions and whether the command was invoked successfully
     * @throws Throwable if an unhandled error occurs while invoking the command
     */
    public CommandInvocationResult<Permission> invoke(Caller caller, Ctx context, String[] args) throws Throwable
    {

      Set<Permission> missingPermissions = null;
      for (final Permission permission : this.permissions)
      {
        if (!caller.hasPermission(permission))
        {
          if (missingPermissions == null)
            missingPermissions = new HashSet<>();
          missingPermissions.add(permission);
        }
      }

      if (missingPermissions != null)
        return CommandInvocationResult.failure(missingPermissions);

      if (!this.subCommandsByAlias.isEmpty() && args.length > 0)
      {
        final Command<Caller, Ctx, Permission> subCommand = this.subCommandOrNull(args[0]);
        if (subCommand != null)
          return subCommand.invoke(caller, context, Arrays.copyOfRange(args, 1, args.length));
      }

      if (this.invoke == null)
        return CommandInvocationResult.successButNoInvoke();

      try {
        final ArgumentMap argumentMap = this.argumentTemplateMap.buildFromValues(args, true);
        this.invoke.invoke(caller, context, argumentMap);
      }
      catch (Throwable e) {
        final CommandException exception;
        if (e instanceof CommandException)
          exception = (CommandException) e;
        else
          exception = new Exceptions.Foreign(e);
        if (this.exceptionHandler != null)
          this.exceptionHandler.handle(exception, caller, context);
        else
          throw e;
      }
      return CommandInvocationResult.success();
    }

    /**
     * Invokes the suggestion handler of the command with the provided caller, context, and arguments
     * @param caller  the caller of the command
     * @param context the context object that will be passed to the command handlers
     * @param args    the arguments of the command
     * @param out the list to which tab completions will be added
     * @return invocation result containing the missing permissions and whether the suggestion handler was called successfully
     * @throws Throwable if an unhandled error occurs while invoking the suggestion handler
     */
    public CommandInvocationResult<Permission> suggest(Caller caller, Ctx context, String[] args, List<String> out)
      throws Throwable
    {

      Set<Permission> missingPermissions = null;
      for (final Permission permission : this.permissions)
      {
        if (!caller.hasPermission(permission))
        {
          if (missingPermissions == null)
            missingPermissions = new HashSet<>();
          missingPermissions.add(permission);
        }
      }

      if (missingPermissions != null)
        return CommandInvocationResult.failure(missingPermissions);

      if (!this.subCommandsByAlias.isEmpty() && args.length > 1)
      {
        final Command<Caller, Ctx, Permission> subCommand = this.subCommandOrNull(args[0]);
        if (subCommand != null)
          return subCommand.suggest(caller, context, Arrays.copyOfRange(args, 1, args.length), out);
      }

      if (this.suggestionHandler != null)
      {
        try {
          final ArgumentMap argumentMap = this.argumentTemplateMap.buildFromValues(args, false);
          this.suggestionHandler.suggest(caller, context, argumentMap, out);
        }
        catch (Throwable e) {
          final CommandException exception;
          if (e instanceof CommandException)
            exception = (CommandException) e;
          else
            exception = new Exceptions.Foreign(e);
          if (this.exceptionHandler != null)
            this.exceptionHandler.handle(exception, caller, context);
          else
            throw e;
        }
      }

      if (args.length > 1)
        return CommandInvocationResult.success();

      final ArrayList<String> completions = new ArrayList<>(this.subCommandsByAlias.size());
      for (final Command<Caller, Ctx, Permission> subCommand : this.subCommands)
      {
        for (final String alias : subCommand.aliases())
        {
          if (alias.startsWith(args.length > 0 ? args[args.length - 1].toLowerCase() : ""))
            completions.add(alias);
        }
      }
      completions.sort(String::compareTo);
      out.addAll(completions);
      return CommandInvocationResult.success();
    }
  }

  /**
   * Represents a caller of a command
   * @param <Permission> the type of the permission object
   */
  public interface Caller<Permission>
  {
    /** @return a caller that has all permissions */
    static <Permission> Caller<Permission> privileged()
    {
      return permission -> true;
    }

    /** @return true if the caller has the specified permission, false otherwise */
    boolean hasPermission(Permission permission);
  }

  /** Represents a handler for a command invocation */
  public interface CommandInvocationHandler<Caller extends Telltale.Caller<Permission>, Ctx, Permission>
  {
    void invoke(Caller caller, Ctx context, ArgumentMap arguments) throws Throwable;
  }

  /** Represents a handler for a command tab completion */
  public interface CommandSuggestionHandler<Caller extends Telltale.Caller<Permission>, Ctx, Permission>
  {
    void suggest(Caller caller, Ctx context, ArgumentMap arguments, List<String> out) throws Throwable;
  }

  /** Represents a handler for a command exception */
  public interface CommandExceptionHandler<Caller extends Telltale.Caller<Permission>, Ctx, Permission>
  {
    void handle(CommandException error, Caller caller, Ctx context);
  }

  /** Represents the result of a command invocation */
  public static final class CommandInvocationResult<Permission>
  {
    private final Set<Permission> missingPermissions;
    private final boolean invoked;

    CommandInvocationResult(Set<Permission> missingPermissions, boolean invoked)
    {
      this.missingPermissions = missingPermissions;
      this.invoked = invoked;
    }

    static <Permission> CommandInvocationResult<Permission> success()
    {
      return new CommandInvocationResult<>(Set.of(), true);
    }

    static <Permission> CommandInvocationResult<Permission> failure(Set<Permission> missingPermissions)
    {
      return new CommandInvocationResult<>(missingPermissions, false);
    }

    static <Permission> CommandInvocationResult<Permission> successButNoInvoke()
    {
      return new CommandInvocationResult<>(Set.of(), false);
    }

    /** @return the set of missing permissions, or an empty set if the command was invoked successfully */
    public Set<Permission> missingPermissions()
    {
      return this.missingPermissions;
    }

    /** @return true if the command was invoked successfully, false otherwise */
    public boolean invoked()
    {
      return this.invoked;
    }
  }

  /** Represents a template for a command argument */
  public static class ArgumentTemplate
  {
    private final boolean required;
    private final String name;
    private final String description;

    ArgumentTemplate(boolean required, String name, String description)
    {
      this.required = required;
      this.name = name;
      this.description = description;
    }

    /** @return a required argument template with the specified name and description */
    public static ArgumentTemplate required(String name, String description)
    {
      return new ArgumentTemplate(true, name, description);
    }

    /** @return an optional argument template with the specified name and description */
    public static ArgumentTemplate optional(String name, String description)
    {
      return new ArgumentTemplate(false, name, description);
    }

    /** @return true if the argument is required, false otherwise */
    public boolean isRequired()
    {
      return this.required;
    }

    /** @return the name of the argument */
    public String name()
    {
      return this.name;
    }

    /** @return the description of the argument */
    public String description()
    {
      return this.description;
    }
  }

  static final class ArgumentTemplateMap
  {
    private final ArgumentTemplate[] templates;

    ArgumentTemplateMap(ArgumentTemplate[] templates) throws Exceptions.DuplicateArgumentTemplateException, Exceptions.RequiredAfterOptionalArgumentException
    {
      this.templates = templates;
      boolean lockRequired = false;
      final HashMap<String, ArgumentTemplate> map = new HashMap<>();
      for (int i = 0; i < templates.length; i++)
      {
        final ArgumentTemplate template = templates[i];
        if (map.containsKey(template.name()))
          throw new Exceptions.DuplicateArgumentTemplateException(template.name());
        if (lockRequired && template.isRequired())
          throw new Exceptions.RequiredAfterOptionalArgumentException(template.name(), i);
        if (!template.isRequired())
          lockRequired = true;
        map.put(template.name(), template);
      }
    }

    ArgumentMap buildFromValues(String[] values, boolean checked) throws Exceptions.TooManyArgumentsException, Exceptions.MissingRequiredArgumentException
    {
      if (checked && values.length > this.templates.length)
        throw new Exceptions.TooManyArgumentsException(values.length, this.templates.length);
      final Argument[] arguments = new Argument[Math.min(values.length, this.templates.length)];
      for (int i = 0; i < arguments.length; i++)
      {
        final var template = this.templates[i];
        arguments[i] = new Argument(i, template, values[i]);
      }
      return this.build(arguments, checked);
    }

    ArgumentMap build(Argument[] arguments, boolean checked) throws Exceptions.MissingRequiredArgumentException
    {
      if (checked && arguments.length < this.templates.length)
      {
        for (int i = arguments.length; i < this.templates.length; i++)
        {
          final var template = this.templates[i];
          if (template.isRequired())
            throw new Exceptions.MissingRequiredArgumentException(template.name(), i);
        }
      }
      return new ArgumentMap(arguments);
    }
  }

  /**
   * A parser for command arguments
   *
   * @param <T> the type of the parsed argument
   */
  public interface ArgumentParser<T>
  {
    /**
     * Parses a command argument
     *
     * @param value the value of the argument
     * @return the parsed argument
     * @throws Throwable if the argument could not be parsed
     */
    T parse(String value) throws Throwable;
  }

  /**
   * Represents a command argument
   */
  public static class Argument
  {
    private final int index;
    private final ArgumentTemplate template;
    private final String value;

    Argument(int index, ArgumentTemplate template, String value)
    {
      this.index = index;
      this.template = template;
      this.value = value;
    }

    /**
     * Parses the argument using the provided parser
     *
     * @param parser the parser to use
     * @param <T>    the type of the parsed argument
     * @return the parsed argument
     * @throws Throwable if the argument could not be parsed
     */
    public <T> T parse(ArgumentParser<T> parser) throws Throwable
    {
      return parser.parse(this.value);
    }

    /**
     * @return the value of the argument as a boolean
     * @throws IllegalArgumentException if the value cannot be parsed as a boolean
     */
    public boolean parseBoolean()
    {
      if (this.value.equalsIgnoreCase("true"))
        return true;
      if (this.value.equalsIgnoreCase("false"))
        return false;
      throw new IllegalArgumentException("invalid boolean value: " + this.value);
    }

    /**
     * @return the value of the argument as an int
     * @throws NumberFormatException if the value cannot be parsed as an int
     */
    public int parseInt() throws NumberFormatException
    {
      return Integer.parseInt(this.value);
    }

    /**
     * @return the value of the argument as a long
     * @throws NumberFormatException if the value cannot be parsed as a long
     */
    public long parseLong() throws NumberFormatException
    {
      return Long.parseLong(this.value);
    }

    /**
     * @return the value of the argument as a float
     * @throws NumberFormatException if the value cannot be parsed as a float
     */
    public float parseFloat() throws NumberFormatException
    {
      return Float.parseFloat(this.value);
    }

    /**
     * @return the value of the argument as a double
     * @throws NumberFormatException if the value cannot be parsed as a double
     */
    public double parseDouble() throws NumberFormatException
    {
      return Double.parseDouble(this.value);
    }

    /** @return the index of the argument in the command invocation */
    public int index()
    {
      return this.index;
    }

    /** @return the template of the argument */
    public ArgumentTemplate template()
    {
      return this.template;
    }

    /** @return the value of the argument */
    public String value()
    {
      return this.value;
    }
  }

  /**
   * A map of command arguments
   */
  public static final class ArgumentMap
  {
    private final Argument[] arr;
    private final HashMap<String, Argument> argumentsByName;

    ArgumentMap(Argument[] arr)
    {
      this.arr = arr;
      final HashMap<String, Argument> map = new HashMap<>();
      for (final Argument arg : arr)
        map.put(arg.template().name(), arg);
      this.argumentsByName = map;
    }

    /**
     * @return the number of arguments in the map
     */
    public int size()
    {
      return this.arr.length;
    }

    /**
     * @param name the name of the argument
     * @return the index of the argument with the specified name, or an empty OptionalInt if no such argument exists
     */
    public OptionalInt indexOf(String name)
    {
      final var arg = this.argumentsByName.get(name);
      if (arg == null)
        return OptionalInt.empty();
      return OptionalInt.of(arg.index());
    }

    /**
     * @param name the name of the argument
     * @return the argument with the specified name
     */
    public Optional<Argument> byName(String name)
    {
      return Optional.ofNullable(this.byNameOrNull(name));
    }

    /**
     * @param name the name of the argument
     * @return the argument with the specified name, or null if no such argument exists
     */
    public Argument byNameOrNull(String name)
    {
      return this.argumentsByName.get(name);
    }

    /**
     * @param index the index of the argument
     * @return the argument at the specified index
     */
    public Optional<Argument> byIndex(int index)
    {
      return Optional.ofNullable(this.byIndexOrNull(index));
    }

    /**
     * @param index the index of the argument
     * @return the argument at the specified index, or null if no such argument exists
     */
    public Argument byIndexOrNull(int index)
    {
      if (index < 0 || index >= this.arr.length)
        return null;
      return this.arr[index];
    }
  }

  /**
   * A registry of commands
   *
   * @param <Caller> the type of the caller of the commands
   * @param <Ctx>    the type of the context object that will be passed to the command handlers
   * @param <Permission> the type of the permission object
   */
  public static class CommandRegistry<Caller extends Telltale.Caller<Permission>, Ctx, Permission>
  {

    public static final class Builder<Caller extends Telltale.Caller<Permission>, Ctx, Permission>
    {
      private final ArrayList<Command<Caller, Ctx, Permission>> commands = new ArrayList<>();

      /**
       * Adds a command to the registry
       *
       * @param command the command to add
       * @return this builder
       */
      public Builder<Caller, Ctx, Permission> withCommand(Command<Caller, Ctx, Permission> command)
      {
        this.commands.add(command);
        return this;
      }

      /**
       * Builds the command registry
       *
       * @return the command registry
       * @throws Exceptions.DuplicateCommandAliasException if two commands in the registry share an alias
       */
      public CommandRegistry<Caller, Ctx, Permission> build() throws Exceptions.DuplicateCommandAliasException
      {
        return new CommandRegistry<>(this.commands.toArray(new Command[0]));
      }
    }

    private final HashMap<String, Command<Caller, Ctx, Permission>> commandsByAlias;
    CommandRegistry(Command<Caller, Ctx, Permission>[] commands) throws Exceptions.DuplicateCommandAliasException
    {
      final HashMap<String, Command<Caller, Ctx, Permission>> commandsByAlias = new HashMap<>(commands.length);
      for (final Command<Caller, Ctx, Permission> command : commands)
      {
        for (final String alias : command.aliases())
        {
          if (commandsByAlias.containsKey(alias))
            throw new Exceptions.DuplicateCommandAliasException(alias);
          commandsByAlias.put(alias, command);
        }
      }
      this.commandsByAlias = commandsByAlias;
    }

    /**
     * Calls a command in the registry
     * @param caller  the caller of the command
     * @param context the context object that will be passed to the command handlers
     * @param args    the arguments of the command, where args[0] is the alias of the command to call
     * @return invocation result containing the missing permissions and whether the command was invoked successfully
     * @throws Throwable if an unhandled exception occurs while calling the command
     */
    public CommandInvocationResult<Permission> call(Caller caller, Ctx context, String[] args) throws Throwable
    {
      if (args.length == 0)
        throw new Exceptions.NoCommandProvidedException();
      final String alias = args[0].toLowerCase();
      final Command<Caller, Ctx, Permission> command = this.commandsByAlias.get(alias);
      if (command == null)
        throw new Exceptions.UnknownCommandException(alias);
      return command.invoke(caller, context, Arrays.copyOfRange(args, 1, args.length));
    }

    /**
     * Calls the suggestion handler of a command in the registry
     *
     * @param caller  the caller of the command
     * @param context the context object that will be passed to the command handlers
     * @param args    the arguments of the command, where args[0] is the alias of the command to call
     * @param buffer  the list to which tab completions will be added
     * @return invocation result containing the missing permissions and whether the suggestion handler was called successfully
     * @throws Throwable if an unhandled exception occurs while calling the command
     */
    public CommandInvocationResult<Permission> suggest(Caller caller, Ctx context, String[] args, List<String> buffer) throws Throwable
    {
      if (args.length == 0)
        throw new Exceptions.NoCommandProvidedException();
      final String alias = args[0].toLowerCase();
      final Command<Caller, Ctx, Permission> command = this.commandsByAlias.get(alias);
      if (command == null)
        throw new Exceptions.UnknownCommandException(alias);
      return command.suggest(caller, context, Arrays.copyOfRange(args, 1, args.length), buffer);
    }

    /** @return the command with the specified alias */
    public Optional<Command<Caller, Ctx, Permission>> get(String alias)
    {
      return Optional.ofNullable(this.getOrNull(alias));
    }

    /** @return the command with the specified alias, or null if no such command exists */
    public Command<Caller, Ctx, Permission> getOrNull(String alias)
    {
      return this.commandsByAlias.get(alias.toLowerCase());
    }
  }

  /**
   * Creates a required argument template with the specified name and description
   *
   * @param name        the name of the argument
   * @param description the description of the argument
   * @return a required argument template
   */
  public static ArgumentTemplate requiredArgument(String name, String description)
  {
    return ArgumentTemplate.required(name, description);
  }

  /**
   * Creates an optional argument template with the specified name and description
   *
   * @param name        the name of the argument
   * @param description the description of the argument
   * @return an optional argument template
   */
  public static ArgumentTemplate optionalArgument(String name, String description)
  {
    return ArgumentTemplate.optional(name, description);
  }

  /**
   * Creates a new command registry builder
   *
   * @param <Caller> the type of the caller of the commands
   * @param <Ctx>    the type of the context object that will be passed to the command handlers
   * @param <Permission> the type of the permission object
   * @return a new command registry builder
   */
  public static <Caller extends Telltale.Caller<Permission>, Ctx, Permission> CommandRegistry.Builder<Caller, Ctx, Permission> newCommandRegistryBuilder()
  {
    return new CommandRegistry.Builder<>();
  }

  /**
   * Creates a new command builder
   *
   * @param <Caller> the type of the caller of the command
   * @param <Ctx>    the type of the context object that will be passed to the command handlers
   * @param <Permission> the type of the permission object
   * @return a new command builder
   */
  public static <Caller extends Telltale.Caller<Permission>, Ctx, Permission> Command.Builder<Caller, Ctx, Permission> newCommandBuilder()
  {
    return new Command.Builder<>();
  }
}
