package com.ytrue.netty.util.internal;

import java.util.*;

import static com.ytrue.netty.util.internal.PriorityQueueNode.INDEX_NOT_IN_QUEUE;

/**
 * @author ytrue
 * @date 2023-07-31 9:54
 * @description 定时任务存储的队列，其实是一个有优先级的任务队列，谁的执行时间快到了，就会被从该队列中取出,这个队列存储数据的结构为数组
 * <p>
 * 数组实现的二叉树
 * <p>
 * Netty 框架中的一个实现了 PriorityQueue 接口的默认优先队列实现类。它使用了二叉堆的数据结构来实现优先队列的功能。
 * DefaultPriorityQueue 类是一个使用二叉堆实现的优先队列类。它提供了添加和删除节点的方法，并通过上浮和下沉操作来维护堆的性质。
 */
public class DefaultPriorityQueue<T extends PriorityQueueNode> extends AbstractQueue<T> implements PriorityQueue<T> {


    /**
     * 初始化一个PriorityQueueNode类型的数组，该PriorityQueueNode类型实际上就ScheduledFutureTask，因为ScheduledFutureTask就实现了PriorityQueueNode接口
     */
    private static final PriorityQueueNode[] EMPTY_ARRAY = new PriorityQueueNode[0];

    /**
     * 任务比较器，比较哪个任务排在队列前面
     */
    private final Comparator<T> comparator;

    /**
     * 队列中真正存储定时任务的数组
     */
    private T[] queue;

    /**
     * 队列中存储的定时任务的个数
     */
    private int size;

    /**
     * 构造
     *
     * @param comparator
     * @param initialSize
     */
    @SuppressWarnings("unchecked")
    public DefaultPriorityQueue(Comparator<T> comparator, int initialSize) {
        // 校验并且赋值
        this.comparator = ObjectUtil.checkNotNull(comparator, "comparator");
        // 如果initialSize 不等0，就创建执行大小的PriorityQueueNode数组，不然就是空
        queue = (T[]) (initialSize != 0 ? new PriorityQueueNode[initialSize] : EMPTY_ARRAY);
    }

    /**
     * 队列的大小，也就是存储定时任务的个数
     *
     * @return
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * 队列是否为空
     *
     * @return
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 队列是否包含该对象
     *
     * @param o element whose presence in this collection is to be tested
     * @return
     */
    @Override
    public boolean contains(Object o) {
        //如果不是PriorityQueueNode类型直接返回false
        if (!(o instanceof PriorityQueueNode)) {
            return false;
        }
        //转换成PriorityQueueNode类型
        PriorityQueueNode node = (PriorityQueueNode) o;
        //node.priorityQueueIndex(this)方法取出该对象在队列数组中的下标
        return contains(node, node.priorityQueueIndex(this));
    }


    /**
     * 队列是否包含该对象
     *
     * @param node
     * @return
     */
    @Override
    public boolean containsTyped(T node) {
        return contains(node, node.priorityQueueIndex(this));
    }

    /**
     * 只把size置为0，但是数组中的定时任务还未被删除，相当于逻辑删除
     */
    @Override
    public void clearIgnoringIndexes() {
        size = 0;
    }

    /**
     * 清空该队列
     */
    @Override
    public void clear() {
        for (int i = 0; i < size; ++i) {
            //取出每一个定时任务
            T node = queue[i];
            if (node != null) {
                //不为null就把元素内部的index设置为INDEX_NOT_IN_QUEU，这就代表着该定时任务不再这个队列中了
                node.priorityQueueIndex(this, INDEX_NOT_IN_QUEUE);
                //把数组的每一个元素置为null
                queue[i] = null;
            }
        }
        size = 0;
    }


    /**
     * 把定时任务添加到任务队列中，其实调用的是add方法，在AbstractQueue类中的方法，但是add方法会调用到offer方法
     *
     * @param e the element to add
     * @return
     */
    @Override
    public boolean offer(T e) {
        //如果该对象的的index不是INDEX_NOT_IN_QUEUE，说明该对象已经在队列中了
        if (e.priorityQueueIndex(this) != INDEX_NOT_IN_QUEUE) {
            throw new IllegalArgumentException("e.priorityQueueIndex(): " + e.priorityQueueIndex(this) +
                    " (expected: " + INDEX_NOT_IN_QUEUE + ") + e: " + e);
        }
        //如果队列存储的定时任务个数已经大于或者等于队列的长度了，就开始扩容
        if (size >= queue.length) {
            queue = Arrays.copyOf(queue, queue.length + ((queue.length < 64) ?
                    (queue.length + 2) :
                    (queue.length >>> 1)));
        }
        //把定时任务添加到任务队列中，这里会先取值，然后再size加1
        bubbleUp(size++, e);
        return true;
    }

    /**
     * 获取队列的第一个定时任务
     *
     * @return
     */
    @Override
    public T poll() {
        if (size == 0) {
            return null;
        }
        T result = queue[0];
        //改变该定时任务的下标，意味着该定时任务要从任务队列中取出了
        result.priorityQueueIndex(this, INDEX_NOT_IN_QUEUE);
        //取出最后一个元素并把size--，这个size--是因为把头部元素取出了，所以要在数量上减1
        T last = queue[--size];
        queue[size] = null;
        if (size != 0) {
            //如果size不为0，就移动元素
            //把后面的元素插入到合适位置，这里用到了小顶堆的数据结构。其实这个任务优先队列，用到的就是这个数据结构。
            bubbleDown(0, last);
        }

        return result;
    }

    /**
     * 获取队列头部的定时任务
     *
     * @return
     */
    @Override
    public T peek() {
        return (size == 0) ? null : queue[0];
    }

    @Override
    public void priorityChanged(T node) {
        int i = node.priorityQueueIndex(this);
        if (!contains(node, i)) {
            return;
        }
        if (i == 0) {
            bubbleDown(i, node);
        } else {
            int iParent = (i - 1) >>> 1;
            T parent = queue[iParent];
            if (comparator.compare(node, parent) < 0) {
                bubbleUp(i, node);
            } else {
                bubbleDown(i, node);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        final T node;
        try {
            node = (T) o;
        } catch (ClassCastException e) {
            return false;
        }
        return removeTyped(node);
    }

    @Override
    public boolean removeTyped(T node) {
        int i = node.priorityQueueIndex(this);
        if (!contains(node, i)) {
            return false;
        }
        node.priorityQueueIndex(this, INDEX_NOT_IN_QUEUE);
        if (--size == 0 || size == i) {
            queue[i] = null;
            return true;
        }
        T moved = queue[i] = queue[size];
        queue[size] = null;
        if (comparator.compare(node, moved) < 0) {
            bubbleDown(i, moved);
        } else {
            bubbleUp(i, moved);
        }
        return true;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(queue, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X[] toArray(X[] a) {
        if (a.length < size) {
            return (X[]) Arrays.copyOf(queue, size, a.getClass());
        }
        System.arraycopy(queue, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public Iterator<T> iterator() {
        return new PriorityQueueIterator();
    }

    /**
     * 内部类，主要是做一个迭代器的实现
     */
    private final class PriorityQueueIterator implements Iterator<T> {
        private int index;

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public T next() {
            if (index >= size) {
                throw new NoSuchElementException();
            }

            return queue[index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }


    /**
     * 移动，也就是重新排列任务队列中的定时任务的优先级
     *
     * @param k
     * @param node
     */
    private void bubbleDown(int k, T node) {
        // 这段代码是一个用于从二叉堆中删除节点的方法。该方法会将最后一个节点放到要删除的位置上，然后逐级下沉，直到满足堆的性质。
        // 首先，方法接收两个参数：k 表示要删除节点的索引，node 表示要删除节点的值。
        // 然后，使用一个循环来进行下沉操作，直到达到堆底或者满足堆的性质。循环条件是 k 小于堆的一半大小，即还未达到堆底。
        // 在循环内部，首先通过位运算计算出左孩子的索引 iChild。然后，获取左孩子的值 child。
        // 接下来，如果右孩子的索引小于堆的大小，说明存在右孩子，需要比较左右孩子的值，选择较小的孩子作为下一次下沉的目标。
        // 然后，通过比较要删除的节点 node 和孩子节点 child 的值，如果要删除的节点的值大于等于孩子节点的值，则说明满足堆的性质，可以退出循环。
        // 如果要删除的节点的值小于孩子节点的值，则需要将孩子节点上移，并更新孩子节点在数组中的索引。具体操作是将孩子节点的值赋给当前节点 k，并调用孩子节点的 priorityQueueIndex 方法，将当前节点的索引更新为 k。
        // 最后，将要删除的节点的值赋给当前节点 k，并调用要删除的节点的 priorityQueueIndex 方法，将当前节点的索引更新为 k。
        // 总结起来，这段代码的作用是从二叉堆中删除指定的节点，并通过下沉操作调整堆的结构，使其满足堆的性质。在下沉过程中，还会更新节点在数组中的索引，以便后续操作可以快速定位到该节点。
        final int half = size >>> 1;
        while (k < half) {
            //在这里会循环取一半
            int iChild = (k << 1) + 1;
            T child = queue[iChild];
            int rightChild = iChild + 1;
            if (rightChild < size && comparator.compare(child, queue[rightChild]) > 0) {
                child = queue[iChild = rightChild];
            }
            if (comparator.compare(node, child) <= 0) {
                break;
            }
            queue[k] = child;
            //在这里就重新设置了定时任务队列中数据的下标
            child.priorityQueueIndex(this, k);
            k = iChild;
        }
        queue[k] = node;
        //在这里就重新设置了定时任务队列中数据的下标
        node.priorityQueueIndex(this, k);
    }

    /**
     * 添加定时任务到任务队列中的核心方法，k是数组存储元素的个数
     *
     * @param k
     * @param node
     */
    private void bubbleUp(int k, T node) {
        // 这段代码是一个用于向二叉堆中插入新节点的方法。该方法会将新节点逐级上浮，直到满足堆的性质。
        // 首先，方法接收两个参数：k 表示新节点的索引，node 表示新节点的值。
        // 然后，使用一个循环来进行上浮操作，直到达到堆顶或者满足堆的性质。循环条件是 k 大于 0，即还未达到堆顶。
        // 在循环内部，首先通过位运算计算出父节点的索引 iParent。然后，获取父节点的值 parent。
        // 接下来，通过比较新节点 node 和父节点 parent 的值，如果新节点的值大于等于父节点的值，则说明满足堆的性质，可以退出循环。
        // 如果新节点的值小于父节点的值，则需要将父节点下移，并更新父节点在数组中的索引。具体操作是将父节点的值赋给当前节点 k，并调用父节点的 priorityQueueIndex 方法，将当前节点的索引更新为 k。
        // 最后，将新节点的值赋给当前节点 k，并调用新节点的 priorityQueueIndex 方法，将当前节点的索引更新为 k。
        while (k > 0) {
            int iParent = (k - 1) >>> 1;
            T parent = queue[iParent];
            if (comparator.compare(node, parent) >= 0) {
                break;
            }
            queue[k] = parent;
            //在这里就设置了定时任务队列中数据的下标
            parent.priorityQueueIndex(this, k);
            k = iParent;
        }
        queue[k] = node;
        //在这里就设置了定时任务队列中数据的下标
        node.priorityQueueIndex(this, k);
    }

    /**
     * 真正判断队列是否包含该定时任务
     *
     * @param node
     * @param i
     * @return
     */
    private boolean contains(PriorityQueueNode node, int i) {
        // 如果i 大于等于0 并且 i 小于任务数组的大小，并且 对象内容是相等的
        // 前面是校验，后面才是判断关键
        return i >= 0 && i < size && node.equals(queue[i]);
    }
}
