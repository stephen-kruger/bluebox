package com.bluebox.smtp.storage;

import com.bluebox.smtp.InboxAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/*
 * This iterator allows transparent stepping through all items in the storage using paging.
 */
public class MessageIterator implements Iterator<BlueboxMessage> {
    private static final Logger log = LoggerFactory.getLogger(MessageIterator.class);
    private static final int MAX = 100;
    private List<BlueboxMessage> list;
    private Iterator<BlueboxMessage> iterator;
    private final InboxAddress address;
    private final BlueboxMessage.State state;
    private int start = 0 - MAX;
    private final long totalCount;
    private long position;
    private int overflowCount;
    private final StorageIf storage;

    public MessageIterator() throws Exception {
        this(null, BlueboxMessage.State.ANY);
    }

    public MessageIterator(InboxAddress address, BlueboxMessage.State state) throws Exception {
        this(StorageFactory.getInstance(), address, state);
    }

    public MessageIterator(StorageIf storage, InboxAddress address, BlueboxMessage.State state) throws Exception {
        this.storage = storage;
        this.address = address;
        this.state = state;
        totalCount = storage.getMailCount(address, state);
        overflowCount = 0;
        position = 0;
        nextPage();
    }

    /*
     * Return percentage of where this iterator is in the overall set of results.
     */
    public int getProgress() {
        return (int) (((position + 1) * 100) / (totalCount + 1));
    }

    private boolean nextPage() throws Exception {
        start += MAX;
        list = storage.listMail(address, state, start, MAX, BlueboxMessage.RECEIVED, true);
        log.debug("Getting page of {} results starting at {} (found {})", MAX, start, list.size());
        iterator = list.iterator();
        return list.size() > 0;
    }

    @Override
    public boolean hasNext() {
        // little check to see we never get into infinite loop
        if ((overflowCount++) > totalCount)
            return false;
        if (!iterator.hasNext()) {
            // check if another page exists
            try {
                return nextPage();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public BlueboxMessage next() {
        position++;
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

}
