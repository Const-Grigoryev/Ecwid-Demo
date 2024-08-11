IPv4 Count
==========

[Problem](https://github.com/Ecwid/new-job/blob/master/IP-Addr-Counter.md)
-------------------------------------------------------------------------

> You have a simple text file with IPv4 addresses. One line is one address, line by line:
> 
>     145.67.23.4
>     8.34.5.23
>     89.54.3.124
>     89.54.3.124
>     3.45.71.5
>     ...
> 
> The file is unlimited in size and can occupy tens and hundreds of gigabytes.
> 
> You should calculate the number of __unique addresses__ in this file using as little memory and time as possible.
> There is a "naive" algorithm for solving this problem (read line by line, put lines into `HashSet`).
> It's better if your implementation is more complicated and faster than this naive algorithm.
> 
> Some details:
>   - For any questions, feel free to write to join-ecom@lightspeedhq.com
>   - You can only use the features of the standard Java/Kotlin library.
>   - You should write in Java (version 17 and above) or Kotlin.
>   - The assignment must have a working `main()` method, to demonstrate how it works
>   - The completed assignment should be posted on GitHub
> 
> ---
> 
> Before submitting an assignment, it will be nice to check how it handles this [file](https://ecwid-vgv-storage.s3.eu-central-1.amazonaws.com/ip_addresses.zip).
> Attention - the file weighs about 20Gb, and unzips to about 120Gb.


Usage
-----

```shell
ipv4_count [FILE...]
```

`FILE` — one or more input files. When started without parameters, the program works in dialog mode, reading data
from the standard input (i.e., from the keyboard).


Implementation notes
--------------------

1. The program can be used either as a user application (`IPv4CountApp` class), or as a library (`IPv4Count` class).
   The `IPv4CountApp` class itself maintains an OS protocol compliance only, and delegates all the real work to the `IPv4Count` engine.

2. The general interface of the application corresponds to that of most Unix utilities:
    - the input file name is passed as the command line argument, or if it is not specified, the program reads from
      the standard input stream;
    - the result is printed to the standard output stream;
    - errors, if they occur, are dumped to `STDERR`;
    - distinct return codes are supported: 0 — everything went fine, –1 — a fatal error occurred, –2 indicates
      an invalid parameter value.

3. The program runs in a single thread, and this is a conscious architectural decision. Measurements show that
   an IO-heavy task on the hardware I have at my disposal will not gain much from parallelization (see
   `BP01_Concurrent` benchmark). Theoretically, the task itself is well suited for a “divide and conquer” strategy,
   and such a solution is possible within the experiment, but it would be hardly practical.

4. The heart of the algorithm is the `BitScale` class. Logically, it is an array of Boolean values, each of which
   corresponds to one possible address from the IPv4 space: “true” means that the address is present in the seen
   portion of the list, “false” means that it is absent. Physically they are packed into “words” of 64 logical units.
   Since the IPv4 address space size is 2³², the entire array occupies 2²⁹ bytes, or 512 MB.

5. Although 512 MB can easily fit into the memory of a modern computer, the code design allows replacing the bit array
   with some more thrifty data structure, for example, a [Bloom filter][Bloom_filter]. In this case, it would be
   possible to reduce a memory consumption (and, probably, a running time — due to a lesser number of cache misses)
   at the cost of counting accuracy.

6. The `BitScale` class can be extended by methods of a standard collection (`List<Boolean>` or `Set<Long>`).
   This extension is not needed by this particular task, as it would require some amount of extra work. However,
   this nest egg was left for the future not without a reason: `BitScale` is a versatile, highly reusable data
   structure.

7. Yet another important component is the IP address parser. It is implemented via a manually constructed
   [finite-state machine][Finite_state_machine] — this is a conventional, well-proven solution for a such kind
   of problem. Alternatives would be:
    - ANTLR or a similar code generation tool;
    - a library of [parser combinators][Parser_combinator];
    - [recursive descent parser][Recursive_descent_parser];
    - spaghetti parser.

8. The parser is written in an old-fashioned C-like manner, with mutable variables, control flags and a large `switch`
   inside. It looks a bit scary, but all this creepiness is encapsulated within a single method and does not spoil
   the code style of the rest of the project. In return, I've managed not to load a garbage collector during program
   execution at all. And this, together with other low-level optimizations, ensured parsing of a single address
   in less than 50 ns (see `BC02_Parser`).


What's next?
------------

That's how can one improve the code or extend the utility's functionality:

- [ ] A proper command line interface. At least the `-h` and `-v` options should be available in every program.

- [ ] Support for different encodings. From the user's side it would look like an optional command line parameter:
  `-e cp1251`, for example. (**UPD:** from version to version, it becomes more and more difficult...)

- [ ] Support wildcard in a file name: `./foo/*.txt`.

- [ ] Another handy option is to enable a fail-fast mode.

- [ ] Extended syntax. Allow comments and whitespace, ~~support IPv6~~ (though no, we'll have to redesign `BitScale`
  for that). There's a room for imagination!

- [ ] Display a line number in an error message.

- [ ] Detailed error messages. For now, we have only three types of them:
    - **Invalid octet value** — octet overflow, semantic error;
    - **Malformed address (too short)** — a line contains less than four octets, syntax error;
    - **Unexpected character** — any other mistake.

- [ ] By way of experiment: processing a file in multiple threads (using `ForkJoinPool` and `FileChannel.map`,
  I suppose). It is not clear, though, whether this will have any practical effect.


Benchmarking
------------

The testing machine had the following configuration:

  - Intel i7-4500U CPU (4 cores @ 1.80 GHz);
  - 8 GB RAM (DDR3 1600 MHz);
  - 2 TB SATA-III SSD;
  - Microsoft Windows 10 + OpenJDK 22.0.2+9-70 (64-bit);
  - Ubuntu Linux 23.10.1 + OpenJDK 22-ea+16-Ubuntu-1 (64-bit).

To estimate the performance of the utility and its parts in different modes, I have written a number of benchmarks.
The main one is called `BI02_Performance` and measures the total program running time during processing of a physical
file of 500 million IP addresses[^dataset-excuses]. According to the experiment, it takes 62–65 s in Windows and
65–71 s in Linux. In more familiar terms, it means overall performance of about **105 MB/s**, or 130 ns per line.

[^dataset-excuses]: Technical limitations do not allow me to use the file proposed in the problem statement: I simply
do not have that much free space. Nevertheless, 500 million addresses (6.64 GB) are enough to evaluate the overall
performance. The only trouble is that the influence of the file system cache can be noticeable at such sizes, so
to get more realistic results one should not run the benchmark several times in a row.


[Bloom_filter]: https://en.wikipedia.org/wiki/Bloom_filter
[Finite_state_machine]: https://en.wikipedia.org/wiki/Finite-state_machine
[Parser_combinator]: https://en.wikipedia.org/wiki/Parser_combinator
[Recursive_descent_parser]: https://en.wikipedia.org/wiki/Recursive_descent_parser
